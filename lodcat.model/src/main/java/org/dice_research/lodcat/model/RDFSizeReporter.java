package org.dice_research.lodcat.model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.dice_research.lodcat.io.CSVWritingDocumentConsumer;
import org.dice_research.lodcat.preproc.NameFilteringSupplierDecorator;
import org.dice_research.topicmodeling.io.FolderReader;
import org.dice_research.topicmodeling.io.factories.StreamOpeningFileBasedDocumentFactory;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.AbstractDocumentProperty;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentInputStream;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes a set of input HDT files
 * and generates a CSV file listing the amount of triples in each input file.
 */
public class RDFSizeReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFSizeReporter.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            LOGGER.info("Usage: RDFSizeReporter <input folder> <output csv file> [<file filter prefix>]");
            return;
        }
        File inputFolder = new File(args[0]);
        File outputFile = new File(args[1]);
        String filenamePrefix = args.length >= 3 ? args[2] : "";
        new RDFSizeReporter().run(inputFolder, outputFile, filenamePrefix);
    }

    protected void run(File inputFolder, File outputFile, String filenamePrefix) {
        LOGGER.trace("Input directory: {}", inputFolder);
        LOGGER.trace("Output file: {}", outputFile);
        LOGGER.trace("Prefix file filter: {}", filenamePrefix);

        FolderReader reader = new FolderReader(
            new StreamOpeningFileBasedDocumentFactory(),
            inputFolder,
            FileFilterUtils.prefixFileFilter(filenamePrefix));
        reader.setUseFolderNameAsCategory(true);
        DocumentSupplier supplier = reader;

        supplier = new NameFilteringSupplierDecorator(supplier, docName -> !docName.getName().contains("squirrel_metadata"));

        // Parse the RDF and store the amount of triples in a document.
        supplier = new AbstractPropertyAppendingDocumentSupplierDecorator<NumberOfTriples>(supplier){
                protected NumberOfTriples createPropertyForDocument(Document document) {
                    try {
                        return new NumberOfTriples(HDTManager.loadHDT(document.getProperty(DocumentInputStream.class).get(), null).getTriples().getNumberOfElements());
                    } catch (IOException e) {
                        LOGGER.error("Exception when parsing HDT file", e);
                        return null;
                    }
                };
        };

        CSVWritingDocumentConsumer consumer = CSVWritingDocumentConsumer.createCSVWritingDocumentConsumer(outputFile, List.of((Class) DocumentName.class, (Class) NumberOfTriples.class));

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
            LOGGER.warn("Got an exception while closing the writer.", e);
        }
    }

    private class NumberOfTriples extends AbstractDocumentProperty {
        private long value;
        public NumberOfTriples(long value) { this.value = value; }
        @Override public Object getValue() { return value; }
    }
}
