package org.dice_research.lodcat.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.dice_research.topicmodeling.algorithms.ClassificationModel;
import org.dice_research.topicmodeling.algorithms.ModelingAlgorithm;
import org.dice_research.topicmodeling.io.CorpusReader;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusReaderDecorator;
import org.dice_research.topicmodeling.io.gzip.GZipProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.java.CorpusObjectReader;
import org.dice_research.topicmodeling.io.ProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.ProbabilisticClassificationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classify documents from a given corpus object file
 * using a previously generated model.
 */
public class ModelClassifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelClassifier.class);

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tModelClassifier <model-file> <corpus-object-file> <output-file>");
        }
        File modelFile = new File(args[0]);
        if (!modelFile.getParentFile().exists()) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }
        File corpusFile = new File(args[1]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }
        File outputFile = new File(args[2]);

        ModelClassifier classifier = new ModelClassifier();
        classifier.run(modelFile, corpusFile, outputFile);
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

    public void run(File modelFile, File corpusFile, File outputFile) {
        LOGGER.trace("Reading model...");
        ClassificationModel model = readClassificationModel(modelFile);

        LOGGER.trace("Reading corpus...");
        Corpus corpus = readCorpus(corpusFile);

        try (
            OutputStream fos = new FileOutputStream(outputFile);
            Writer osw = new OutputStreamWriter(fos);
            Writer writer = new BufferedWriter(osw)
        ) {
            for (Document document : corpus) {
                String documentName = document.getProperty(DocumentName.class).getStringValue();
                LOGGER.trace("Document name: {}", documentName);

                String documentURI = document.getProperty(DocumentURI.class).getStringValue();
                LOGGER.trace("Document URI: {}", documentURI);

                ProbabilisticClassificationResult result = (ProbabilisticClassificationResult) model.getClassificationForDocument(document);
                LOGGER.trace("Classification result: {}", result);

                StringBuilder s = new StringBuilder();
                s.append("\"");
                s.append(documentName);
                s.append("\",\"");
                s.append(documentURI);
                s.append("\",");
                s.append(result.getClassId());
                for (double topicProbability : result.getTopicProbabilities()) {
                    s.append(",");
                    s.append(topicProbability);
                }
                s.append("\n");
                writer.write(s.toString());
            }
        } catch (IOException e) {
            LOGGER.error("Exception while writing", e);
        }
    }
}
