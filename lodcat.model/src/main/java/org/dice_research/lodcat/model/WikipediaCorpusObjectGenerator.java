package org.dice_research.lodcat.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dice_research.lodcat.preproc.DocumentNameFileFilter;
import org.dice_research.lodcat.preproc.VocabularyAddingWordIndexingSupplierDecorator;
import org.dice_research.topicmodeling.io.CorpusWriter;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusWriterDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectWriter;
import org.dice_research.topicmodeling.io.xml.stream.XmlPartsBasedDocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.ListCorpusCreator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentTextWithTermInfoParsingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentWordCountingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.corpus.DocumentListCorpus;
import org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentProperty;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentTextWordIds;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.SimpleVocabulary;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikipediaCorpusObjectGenerator implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaCorpusObjectGenerator.class);

    private File inputDirectory;
    private File outputFile;
    private File nameFilterFile;

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(Option.builder("i").longOpt("input").hasArg().type(File.class).required().desc("Input directory").build());
        options.addOption(Option.builder("o").longOpt("output").hasArg().type(File.class).required().desc("Output file").build());
        options.addOption(Option.builder("n").longOpt("name-filter").hasArg().type(File.class).desc("File with a list of document names to filter out").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine line = parser.parse(options, args);

        File inputDirectory = (File) line.getParsedOptionValue("input");
        if (!inputDirectory.exists()) {
            throw new IllegalArgumentException("The given input directory does not exist.");
        }
        File outputFile = (File) line.getParsedOptionValue("output");
        File nameFilterFile = (File) line.getParsedOptionValue("name-filter");

        (new WikipediaCorpusObjectGenerator(inputDirectory, outputFile, nameFilterFile)).run();
    }

    public WikipediaCorpusObjectGenerator(File inputDirectory, File outputFile, File nameFilterFile) {
        this.inputDirectory = inputDirectory;
        this.outputFile = outputFile;
        this.nameFilterFile = nameFilterFile;
    }

    @Override
    public void run() {
        DocumentSupplier supplier;
        supplier = new XmlPartsBasedDocumentSupplier(inputDirectory);

        // Filter out documents by title
        if (nameFilterFile != null) {
            supplier = new DocumentFilteringSupplierDecorator(supplier, new DocumentNameFileFilter(nameFilterFile));
        }

        // Parse the term tokenized text from the document text
        supplier = new DocumentTextWithTermInfoParsingSupplierDecorator(supplier);

        // Index words
        Vocabulary vocabulary = new SimpleVocabulary();
        supplier = new VocabularyAddingWordIndexingSupplierDecorator(supplier, vocabulary);
        // Create Bag of words representation
        supplier = new DocumentWordCountingSupplierDecorator(supplier);

        // Since this property is not serializeable we have to remove it
        List<Class<? extends DocumentProperty>> propertiesToRemove = new ArrayList<Class<? extends DocumentProperty>>();
        propertiesToRemove.add(DocumentText.class);
        propertiesToRemove.add(TermTokenizedText.class);
        propertiesToRemove.add(DocumentTextWordIds.class);
        supplier = new PropertyRemovingSupplierDecorator(supplier, propertiesToRemove);

        ListCorpusCreator<List<Document>> preprocessor = new ListCorpusCreator<List<Document>>(supplier,
                new DocumentListCorpus<List<Document>>(new ArrayList<Document>()));
        LOGGER.info("Processing corpus...");
        Corpus corpus = preprocessor.getCorpus();
        corpus.addProperty(new CorpusVocabulary(vocabulary));

        LOGGER.info("Writing corpus...");
        CorpusWriter writer = new GZipCorpusWriterDecorator(new CorpusObjectWriter());
        try {
            writer.writeCorpus(corpus, outputFile);
        } catch (IOException e) {
            LOGGER.error("Exception while writing corpus. Aborting.", e);
            return;
        }
        LOGGER.info("Done.");
    }

}
