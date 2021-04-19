package org.dice_research.lodcat.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.dice_research.lodcat.preproc.TextProcessingSupplierDecorator;
import org.dice_research.topicmodeling.algorithms.ClassificationModel;
import org.dice_research.topicmodeling.algorithms.ModelingAlgorithm;
import org.dice_research.topicmodeling.algorithms.ProbTopicModelingAlgorithmStateSupplier;
import org.dice_research.topicmodeling.io.ProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.gzip.GZipProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.xml.stream.StreamBasedXmlDocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.ListCorpusCreator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.corpus.DocumentListCorpus;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.ProbabilisticClassificationResult;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classify documents from a given corpus XML file (or a directory with such files)
 * using a previously generated gzipped model.
 */
public class ModelClassifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelClassifier.class);

    private ClassificationModel model;

    private Vocabulary vocabulary;

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

    private void readClassificationModel(File modelFile) {
        ProbTopicModelingAlgorithmStateReader reader = new GZipProbTopicModelingAlgorithmStateReader();
        ProbTopicModelingAlgorithmStateSupplier algorithmStateSupplier = reader.readProbTopicModelState(modelFile);
        vocabulary = algorithmStateSupplier.getVocabulary();
        model = (ClassificationModel) (((ModelingAlgorithm) algorithmStateSupplier).getModel());
    }

    private Stream<Document> processCorpus(File corpusFile) {
        LOGGER.info("Corpus: {}", corpusFile);
        DocumentSupplier supplier = StreamBasedXmlDocumentSupplier.createReader(corpusFile, false);

        supplier = new TextProcessingSupplierDecorator(supplier, vocabulary);

        return DocumentSupplier.convertToStream(supplier);
    }

    private Document processDocument(Document document) {
        DocumentName documentName = document.getProperty(DocumentName.class);
        LOGGER.trace("{}", documentName);

        DocumentURI documentURI = document.getProperty(DocumentURI.class);
        LOGGER.trace("{}", documentURI);

        ProbabilisticClassificationResult result = (ProbabilisticClassificationResult) model.getClassificationForDocument(document);
        LOGGER.trace("Classification result: {}", result);
        document.addProperty(result);

        return document;
    }

    public void run(File modelFile, File corpusFile, File outputFile) {
        LOGGER.info("Reading model: {}", modelFile);
        readClassificationModel(modelFile);

        Stream<File> corpusFiles;
        if (corpusFile.isDirectory()) {
            corpusFiles = Streams.stream(FileUtils.iterateFiles(corpusFile, FileFilterUtils.suffixFileFilter(".xml"), null));
        } else {
            corpusFiles = Stream.of(corpusFile);
        }

        Stream<Document> documents = corpusFiles.flatMap(this::processCorpus);

        LOGGER.info("Processing documents...");
        try (
            OutputStream fos = new FileOutputStream(outputFile);
            Writer osw = new OutputStreamWriter(fos);
            Writer writer = new BufferedWriter(osw)
        ) {
            documents.map(this::processDocument).map(document -> {
                DocumentName documentName = document.getProperty(DocumentName.class);
                DocumentURI documentURI = document.getProperty(DocumentURI.class);
                ProbabilisticClassificationResult result = document.getProperty(ProbabilisticClassificationResult.class);

                StringBuilder s = new StringBuilder();
                s.append("\"");
                s.append(documentName != null ? documentName.getStringValue() : "");
                s.append("\",\"");
                s.append(documentURI != null ? documentURI.getStringValue() : "");
                s.append("\",");
                s.append(result.getClassId());
                for (double topicProbability : result.getTopicProbabilities()) {
                    s.append(",");
                    s.append(topicProbability);
                }
                s.append("\n");
                return s.toString();
            }).forEach(s -> {
                try {
                    writer.write(s);
                } catch (IOException e) {
                    LOGGER.error("Exception while writing", e);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Exception while writing", e);
        }

        LOGGER.info("Done");
    }
}
