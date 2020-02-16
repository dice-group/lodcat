package org.dice_research.lodcat.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.dice_research.lodcat.preproc.JenaBasedParsingSupplierDecorator;
import org.dice_research.topicmodeling.io.FolderReader;
import org.dice_research.topicmodeling.io.xml.XmlWritingDocumentConsumer;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentTextCreatingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.StringContainingDocumentPropertyBasedFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.StringContainingDocumentPropertyBasedFilter.StringContainingDocumentPropertyBasedFilterType;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentRawData;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes a metadata graph, a set of input files (i.e., located in a
 * directory) and generates an initial corpus representation of the dataset by
 * using a service for retrieving labels and descriptions for the URIs used.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class InitialCorpusGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialCorpusGenerator.class);
    
    protected void run(File inputFolder, File corpusFile) {
        FolderReader reader = new FolderReader(inputFolder);
        reader.setUseFolderNameAsCategory(true);
        DocumentSupplier supplier = reader;
        // Remove all files which do not end with .ttl
        supplier = new DocumentFilteringSupplierDecorator(supplier, new StringContainingDocumentPropertyBasedFilter<>(
                StringContainingDocumentPropertyBasedFilterType.ENDS_WITH, DocumentName.class, ".ttl", true));
        
        supplier = new DocumentTextCreatingSupplierDecorator(supplier);
        supplier = new JenaBasedParsingSupplierDecorator(supplier);
        supplier = new PropertyRemovingSupplierDecorator(supplier,
                Arrays.asList(DocumentRawData.class, DocumentText.class));

        XmlWritingDocumentConsumer consumer = XmlWritingDocumentConsumer
                .createXmlWritingDocumentConsumer(corpusFile.getAbsoluteFile());

        Document document = supplier.getNextDocument();
        int count = 0;
        while (document != null) {
            try {
                consumer.consumeDocument(document);
            } catch (Exception e) {
                LOGGER.error("Exception at document #" + document.getDocumentId() + ". Aborting.", e);
                return;
            }
            ++count;
            if ((count % 100) == 0) {
                LOGGER.info("Saw " + count + " documents");
            }
            document = supplier.getNextDocument();
        }
        LOGGER.info("Saw " + count + " documents");
        try {
            consumer.close();
        } catch (IOException e) {
            LOGGER.warn("Got an exception while closing the XML Writer.", e);
        }
    }
}
