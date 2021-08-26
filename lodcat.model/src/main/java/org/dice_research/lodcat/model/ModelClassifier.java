package org.dice_research.lodcat.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.dice_research.lodcat.preproc.TextProcessingSupplierDecorator;
import org.dice_research.topicmodeling.algorithms.ClassificationModel;
import org.dice_research.topicmodeling.algorithms.ModelingAlgorithm;
import org.dice_research.topicmodeling.algorithms.ProbTopicModelingAlgorithmStateSupplier;
import org.dice_research.topicmodeling.io.ProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.gzip.GZipProbTopicModelingAlgorithmStateReader;
import org.dice_research.topicmodeling.io.xml.stream.StreamBasedXmlDocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
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
        if ((!outputFile.getParentFile().exists()) && (!outputFile.getParentFile().mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }

        ModelClassifier classifier = new ModelClassifier();
        classifier.run(modelFile, corpusFile, outputFile);
    }

    private void readClassificationModel(File modelFile) {
        ProbTopicModelingAlgorithmStateReader reader = new GZipProbTopicModelingAlgorithmStateReader();
        ProbTopicModelingAlgorithmStateSupplier algorithmStateSupplier = reader.readProbTopicModelState(modelFile);
        vocabulary = algorithmStateSupplier.getVocabulary();
        model = (ClassificationModel) (((ModelingAlgorithm) algorithmStateSupplier).getModel());
    }

    private DocumentSupplier readCorpus(File corpusFile) {
        LOGGER.info("Corpus: {}", corpusFile);
        DocumentSupplier supplier = StreamBasedXmlDocumentSupplier.createReader(corpusFile, false);

        supplier = new DocumentFilteringSupplierDecorator(supplier, document -> {
                DocumentName documentName = document.getProperty(DocumentName.class);
                DocumentURI documentURI = document.getProperty(DocumentURI.class);
                LOGGER.trace("Processing: {} ({})...", documentName, documentURI);
                return true;
        });

        supplier = new TextProcessingSupplierDecorator(supplier, vocabulary, false);

        return supplier;
    }

    public void run(File modelFile, File corpusFile, File outputFile) {
        LOGGER.info("Reading model: {}", modelFile);
        readClassificationModel(modelFile);

        Stream<File> corpora;
        if (corpusFile.isDirectory()) {
            Iterator<File> files = FileUtils.iterateFiles(corpusFile, FileFilterUtils.suffixFileFilter(".xml"), null);
            corpora = Stream.generate(() -> files.hasNext() ? files.next() : null).takeWhile(file -> file != null).parallel();
        } else {
            corpora = Stream.of(corpusFile);
        }

        LOGGER.info("Processing documents...");
        try (
            OutputStream fos = new FileOutputStream(outputFile);
            Writer osw = new OutputStreamWriter(fos);
            Writer writer = new BufferedWriter(osw)
        ) {
            corpora.map(this::readCorpus).map(DocumentSupplierAsIterator::new).forEach(documents -> {
                    while (documents.hasNext()) {
                        Document document = documents.next();

                        DocumentName documentName = document.getProperty(DocumentName.class);
                        DocumentURI documentURI = document.getProperty(DocumentURI.class);
                        ProbabilisticClassificationResult result = (ProbabilisticClassificationResult) model
                            .getClassificationForDocument(document);

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

                        LOGGER.trace("Processed: {} ({})", documentName, documentURI);

                        try {
                            synchronized (this) {
                                writer.write(s.toString());
                            }
                        } catch (IOException e) {
                            LOGGER.error("Exception while writing: {} ({})", documentName, documentURI, e);
                        }
                    }
            });
        } catch (IOException e) {
            LOGGER.error("Exception while writing", e);
        }

        LOGGER.info("Done");
    }
}
