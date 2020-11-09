package org.dice_research.lodcat.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.jena.vocabulary.RDF;
import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.lodcat.preproc.JenaBasedParsingSupplierDecorator;
import org.dice_research.lodcat.preproc.NameFilteringSupplierDecorator;
import org.dice_research.lodcat.preproc.SQLUriVerbalizingSupplierDecorator;
import org.dice_research.lodcat.preproc.SquirrelMetadataAddingSupplierDecorator;
import org.dice_research.lodcat.preproc.TextCleaningSupplierDecorator;
import org.dice_research.lodcat.preproc.UriFilteringSupplierDecorator;
import org.dice_research.lodcat.uri.UriNamespaceFilter;
import org.dice_research.topicmodeling.io.FolderReader;
import org.dice_research.topicmodeling.io.factories.StreamOpeningFileBasedDocumentFactory;
import org.dice_research.topicmodeling.io.xml.XmlWritingDocumentConsumer;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.GZipExtractingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentInputStream;
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

    private static final String[] BLACKLISTED_NAMESPACES = new String[] { RDF.getURI() };

    public static void main(String[] args) {
        new InitialCorpusGenerator().run(new File(args[0]), new File(args[1]));
    }

    protected void run(File inputFolder, File corpusFile) {
        FolderReader reader = new FolderReader(new StreamOpeningFileBasedDocumentFactory(), inputFolder);
        reader.setUseFolderNameAsCategory(true);
        DocumentSupplier supplier = reader;

        supplier = new NameFilteringSupplierDecorator(supplier, docName -> !docName.getName().contains("squirrel_metadata"));

        supplier = SquirrelMetadataAddingSupplierDecorator.create(supplier, inputFolder);

        // FIXME clarify whether we need this step. Might be possible, that we have .gz
        // files
        // supplier = new GZipExtractingSupplierDecorator(supplier);

        // FIXME the following filtering step is not necessary since the FolderReader
        // should be able to use an IOFilter
        // Remove all files which do not end with .ttl
//        supplier = new DocumentFilteringSupplierDecorator(supplier, new StringContainingDocumentPropertyBasedFilter<>(
//                StringContainingDocumentPropertyBasedFilterType.ENDS_WITH, DocumentName.class, ".ttl", true));

        // Transform data into text
        // supplier = new DocumentTextCreatingSupplierDecorator(supplier,
        // StandardCharsets.UTF_8);

        // Parse the RDF and keep a map of URIs to their counts
        supplier = new JenaBasedParsingSupplierDecorator(supplier);

        // Filter URIs based on their namespace
        supplier = new UriFilteringSupplierDecorator(supplier, new UriNamespaceFilter(BLACKLISTED_NAMESPACES, true));

        // Remove unnecessary properties
        supplier = new PropertyRemovingSupplierDecorator(supplier,
                Arrays.asList(DocumentInputStream.class, DocumentRawData.class, DocumentText.class));

        supplier = new SQLUriVerbalizingSupplierDecorator(supplier, new String[]{"label", "description"});

        supplier = new TextCleaningSupplierDecorator(supplier);

        XmlWritingDocumentConsumer consumer = XmlWritingDocumentConsumer
                .createXmlWritingDocumentConsumer(corpusFile.getAbsoluteFile());

        consumer.registerParseableDocumentProperty(UriCounts.class);

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
