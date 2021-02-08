package org.dice_research.lodcat.model;

import org.dice_research.topicmodeling.utils.doc.DocumentName;
import java.io.File;
import org.dice_research.topicmodeling.algorithms.ClassificationModel;
import org.dice_research.topicmodeling.algorithms.ModelingAlgorithm;
import org.dice_research.topicmodeling.io.CorpusReader;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusReaderDecorator;
import org.dice_research.topicmodeling.io.gzip.GZipProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.java.CorpusObjectReader;
import org.dice_research.topicmodeling.io.ProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classify documents from a given corpus object file
 * using a previously generated model.
 */
public class ModelClassifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelClassifier.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tModelClassifier <model-file> <corpus-object-file>");
        }
        File modelFile = new File(args[0]);
        if (!modelFile.getParentFile().exists()) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }
        File corpusFile = new File(args[1]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }

        ModelClassifier classifier = new ModelClassifier();
        classifier.run(modelFile, corpusFile);
    }

    private ClassificationModel readClassificationModel(File modelFile) {
        ProbTopicModelingAlgorithmStateReader reader = new GZipProbTopicModelingAlgorithmStateReader();
        ModelingAlgorithm algorithm = (ModelingAlgorithm) reader.readProbTopicModelState(modelFile);
        return (ClassificationModel) algorithm.getModel();
    }

    private Corpus readCorpus(File corpusFile) {
        CorpusReader reader = new GZipCorpusReaderDecorator(new CorpusObjectReader());
        reader.readCorpus(corpusFile);
        return reader.getCorpus();
    }

    public void run(File modelFile, File corpusFile) {
        LOGGER.trace("Reading model...");
        ClassificationModel model = readClassificationModel(modelFile);

        LOGGER.trace("Reading corpus...");
        Corpus corpus = readCorpus(corpusFile);

        for (Document document : corpus) {
            LOGGER.trace("Document name: {}", document.getProperty(DocumentName.class));

            DocumentClassificationResult result = model.getClassificationForDocument(document);
            LOGGER.trace("Classification result: {}", result);
        }
    }
}
