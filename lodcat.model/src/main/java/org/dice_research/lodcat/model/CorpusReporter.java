package org.dice_research.lodcat.model;

import java.io.IOException;
import java.io.FileWriter;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary;
import java.io.File;

import org.dice_research.topicmodeling.io.CorpusReader;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusReaderDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectReader;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple corpus reporter which prints information about the given preprocessed corpus.
 */
public class CorpusReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorpusReporter.class);

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tCorpusReporter <corpus-file>");
        }
        File corpusFile = new File(args[0]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }

        new CorpusReporter().run(corpusFile, new File("corpus-report"));
    }

    public void run(File corpusFile, File reportFile) throws IOException {
        CorpusReader reader = new GZipCorpusReaderDecorator(new CorpusObjectReader());
        LOGGER.debug("Reading corpus...");
        reader.readCorpus(corpusFile);
        Corpus corpus = reader.getCorpus();
        if (corpus == null) {
            LOGGER.error("Couldn't load corpus from file. Aborting.");
            return;
        }
        Vocabulary vocab = corpus.getProperty(CorpusVocabulary.class).getVocabulary();

        LOGGER.debug("Writing report...");
        try (FileWriter writer = new FileWriter(reportFile)) {
            for (String word : vocab) {
                writer.write(word);
                writer.write("\n");
            }
        }
    }
}
