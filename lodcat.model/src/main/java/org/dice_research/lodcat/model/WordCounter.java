package org.dice_research.lodcat.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
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
import org.dice_research.topicmodeling.io.xml.XmlWritingDocumentConsumer;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplierAsIterator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.DocumentWordCounts;
import org.dice_research.topicmodeling.utils.doc.ProbabilisticClassificationResult;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process documents from a given corpus XML file (or a directory with files)
 * using a previosly generated model.
 * Generates an XML file with word counts.
 */
public class WordCounter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordCounter.class);

    private ClassificationModel model;

    private Vocabulary vocabulary;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tWordCounter <model-file> <input-file-or-dir> <output-dir>");
        }
        File modelFile = new File(args[0]);
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("The given model does not exist.");
        }
        File corpusFile = new File(args[1]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }
        File outputDir = new File(args[2]);
        if ((!outputDir.exists()) && (!outputDir.mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }

        new WordCounter().run(modelFile, corpusFile, outputDir);
    }

    private void readClassificationModel(File modelFile) {
        ProbTopicModelingAlgorithmStateReader reader = new GZipProbTopicModelingAlgorithmStateReader();
        ProbTopicModelingAlgorithmStateSupplier algorithmStateSupplier = reader.readProbTopicModelState(modelFile);
        vocabulary = algorithmStateSupplier.getVocabulary();
        model = (ClassificationModel) (((ModelingAlgorithm) algorithmStateSupplier).getModel());
    }

    public void run(File modelFile, File corpusFile, File outputDir) {
        LOGGER.info("Reading model: {}", modelFile);
        readClassificationModel(modelFile);
    LOGGER.info("Vocabulary size: {}", vocabulary.size());

        Stream<File> corpora;
    Iterator<File> inputFiles;
        if (corpusFile.isDirectory()) {
            inputFiles = FileUtils.iterateFiles(corpusFile, FileFilterUtils.suffixFileFilter(".xml"), null);
        } else {
        inputFiles = Arrays.asList(corpusFile).iterator();
        }

    ExecutorService executorService = Executors.newFixedThreadPool(4); // FIXME number of jobs
    for (File file; inputFiles.hasNext();) {
        file = inputFiles.next();
        // FIXME duplicate file names in different directories in input
        executorService.submit(new ProcessCorpusTask(file, outputDir.toPath().resolve(file.getName()).toFile()));
    }
        executorService.shutdown();
    }

    protected class ProcessCorpusTask implements Runnable {
        private File corpusFile;
        private File outputFile;

        public ProcessCorpusTask(File corpusFile, File outputFile) {
            this.corpusFile = corpusFile;
            this.outputFile = outputFile;
        }

        @Override
        public void run() {
            LOGGER.info("Corpus: {}", corpusFile);
            DocumentSupplier supplier = StreamBasedXmlDocumentSupplier.createReader(corpusFile, false);

            supplier = new DocumentFilteringSupplierDecorator(supplier, document -> {
                DocumentName documentName = document.getProperty(DocumentName.class);
                DocumentURI documentURI = document.getProperty(DocumentURI.class);
                LOGGER.trace("Processing: {} ({})...", documentName, documentURI);
                return true;
            });

            supplier = new TextProcessingSupplierDecorator(supplier, vocabulary, false);

            supplier = new PropertyRemovingSupplierDecorator(supplier, Arrays.asList(DocumentText.class));

            try (XmlWritingDocumentConsumer consumer = XmlWritingDocumentConsumer.createXmlWritingDocumentConsumer(outputFile.getAbsoluteFile())) {
                consumer.registerParseableDocumentProperty(DocumentWordCounts.class);
                while (true) {
                    Document document = supplier.getNextDocument();
                    if (document == null) break;

                    try {
                        consumer.consumeDocument(document);
                    } catch (Exception e) {
                        LOGGER.error("Exception at document #" + document.getDocumentId() + ". Aborting.", e);
                        return;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Exception", e);
            }
        }
    }
}
