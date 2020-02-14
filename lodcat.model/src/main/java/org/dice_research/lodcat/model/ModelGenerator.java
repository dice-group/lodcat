package org.dice_research.lodcat.model;

import java.io.File;

import org.dice_research.topicmodeling.algorithm.mallet.MalletLdaWrapper;
import org.dice_research.topicmodeling.algorithms.ModelingAlgorithm;
import org.dice_research.topicmodeling.algorithms.ProbTopicModelingAlgorithmStateSupplier;
import org.dice_research.topicmodeling.io.CorpusReader;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusReaderDecorator;
import org.dice_research.topicmodeling.io.gzip.GZipProbTopicModelingAlgorithmStateWriter;
import org.dice_research.topicmodeling.io.java.CorpusObjectReader;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple model generator. Note that this class solely executes the modeling
 * algorithm. It is expected that the given corpus is already preprocessed. The
 * generate file contains the last state of the algorithm.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ModelGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelGenerator.class);

//    private static final int NUMBER_OF_TOPICS = 1000;
    private static final int NUMBER_OF_STEPS = 1040;
//    private static final String MODEL_OBJECT_FILE = MODEL_FOLDER + File.separator + "probAlgState.object";
//    private static final String CORPUS_FILE = "/home/mroeder/tapioca/lodStats_all_log.object";

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tModelGenerator <corpus-file> <output-file> <#topics>");
        }
        int numberOfTopics = Integer.parseInt(args[2]);
        File corpusFile = new File(args[0]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }
        File modelFile = new File(args[1]);
        if ((!modelFile.getParentFile().exists()) && (!modelFile.getParentFile().mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }

        ModelGenerator generator = new ModelGenerator(numberOfTopics, NUMBER_OF_STEPS);
        generator.run(corpusFile, modelFile);
    }

    private int numberOfTopics;
    private int numberOfSteps;

    public ModelGenerator(int numberOfTopics, int numberOfSteps) {
        this.numberOfTopics = numberOfTopics;
        this.numberOfSteps = numberOfSteps;
    }

    public void run(File corpusFile, File modelFile) {
        CorpusReader reader = new GZipCorpusReaderDecorator(new CorpusObjectReader());
        LOGGER.info("Reading corpus...");
        reader.readCorpus(corpusFile);
        Corpus corpus = reader.getCorpus();
        if (corpus == null) {
            LOGGER.error("Couldn't load corpus from file. Aborting.");
            return;
        }
        ModelingAlgorithm algorithm = new MalletLdaWrapper(numberOfTopics);
        LOGGER.info("Initializing algorithm...");
        algorithm.initialize(corpus);
        corpus = null;
        LOGGER.info("Executing algorithm...");
        for (int i = 0; i < numberOfSteps; ++i) {
            algorithm.performNextStep();
            if ((i % 100) == 0) {
                LOGGER.info("Performed step #{}", i);
            }
        }
        LOGGER.info("Writing state file...");
        GZipProbTopicModelingAlgorithmStateWriter writer = new GZipProbTopicModelingAlgorithmStateWriter();
        writer.writeProbTopicModelState((ProbTopicModelingAlgorithmStateSupplier) algorithm, modelFile);
        LOGGER.info("Done.");
    }
}
