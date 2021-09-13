package org.dice_research.lodcat.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.dice_research.topicmodeling.io.xml.XmlBasedCorpusPartWriter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.wikipedia.WikipediaDumpReader;
import org.dice_research.topicmodeling.wikipedia.WikipediaMarkupDeletingDocumentSupplierDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits a single XML Wikipedia file to multiple XML corpus files.
 */
public class WikipediaCorpusPartitioner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaCorpusPartitioner.class);

    private File inputCorpusFile;
    private File outputDirectory;
    private int documentPerPart;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tXmlBasedCorpusPartitioner <wikipedia-xml-file> <output-dir> <doc-per-part>");
        }
        File inputCorpusFile = new File(args[0]);
        if (!inputCorpusFile.exists()) {
            throw new IllegalArgumentException("The given wikipedia file does not exist.");
        }
        File outputDirectory = new File(args[1]);
        if ((!outputDirectory.exists()) && (!outputDirectory.mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }
        int documentPerPart = Integer.parseInt(args[2]);

        (new WikipediaCorpusPartitioner(inputCorpusFile, outputDirectory, documentPerPart)).run();
    }

    public WikipediaCorpusPartitioner(File inputCorpusFile, File outputDirectory, int documentPerPart) {
        this.inputCorpusFile = inputCorpusFile;
        this.outputDirectory = outputDirectory;
        this.documentPerPart = documentPerPart;
    }

    @Override
    public void run() {
        DocumentSupplier supplier;
        XmlBasedCorpusPartWriter writer = new XmlBasedCorpusPartWriter(outputDirectory, documentPerPart);
        try (InputStream input = new BZip2CompressorInputStream(new FileInputStream(inputCorpusFile), true)) {
            supplier = WikipediaDumpReader.createReader(input, StandardCharsets.UTF_8);
            // Remove Wikipedia markup
            supplier = new WikipediaMarkupDeletingDocumentSupplierDecorator(supplier);

            int count = 0;
            Document document = supplier.getNextDocument();
            while (document != null) {
                writer.consumeDocument(document);
                ++count;
                if ((count % 100) == 0) {
                    LOGGER.info("Saw {} documents", count);
                }
                document = supplier.getNextDocument();
            }
            LOGGER.info("Saw {} documents", count);
        } catch (IOException e) {
            LOGGER.error("Error while reading Wikipedia file: {}", e);
        } finally {
            writer.close();
        }
        LOGGER.info("Done.");
    }

}
