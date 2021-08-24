package org.dice_research.lodcat.model;

import java.io.File;

import org.dice_research.topicmodeling.io.xml.stream.StreamBasedXmlDocumentSupplier;
import org.dice_research.topicmodeling.io.xml.XmlBasedCorpusPartWriter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits a single XML corpus file to multiple XML corpus files.
 */
public class XmlBasedCorpusPartitioner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlBasedCorpusPartitioner.class);

    private File inputCorpusFile;
    private File outputDirectory;
    private int documentPerPart;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tXmlBasedCorpusPartitioner <xml-corpus-file> <output-dir> <doc-per-part>");
        }
        File inputCorpusFile = new File(args[0]);
        if (!inputCorpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }
        File outputDirectory = new File(args[1]);
        if ((!outputDirectory.exists()) && (!outputDirectory.mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }
        int documentPerPart = Integer.parseInt(args[2]);

        new XmlBasedCorpusPartitioner(inputCorpusFile, outputDirectory, documentPerPart).run();
    }

    public XmlBasedCorpusPartitioner(File inputCorpusFile, File outputDirectory, int documentPerPart) {
        this.inputCorpusFile = inputCorpusFile;
        this.outputDirectory = outputDirectory;
        this.documentPerPart = documentPerPart;
    }

    @Override
    public void run() {
        DocumentSupplier supplier;
        supplier = StreamBasedXmlDocumentSupplier.createReader(inputCorpusFile, true);

        XmlBasedCorpusPartWriter consumer;
        consumer = new XmlBasedCorpusPartWriter(outputDirectory, documentPerPart);

        int count = 0;
        while (true) {
            Document document = supplier.getNextDocument();
            if (document == null) break;
            consumer.consumeDocument(document);
            ++count;
            if ((count % 100) == 0) {
                LOGGER.info("Saw {} documents", count);
            }
        }
        LOGGER.info("Saw {} documents", count);
        consumer.close();
        LOGGER.info("Done.");
    }

}
