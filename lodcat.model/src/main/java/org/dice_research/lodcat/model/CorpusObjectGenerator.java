package org.dice_research.lodcat.model;

import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.util.PropertiesUtils;
import org.aksw.simba.tapioca.data.SimpleTokenizedText;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.dice_research.topicmodeling.io.CorpusWriter;
import org.dice_research.topicmodeling.io.gzip.GZipCorpusWriterDecorator;
import org.dice_research.topicmodeling.io.java.CorpusObjectWriter;
import org.dice_research.topicmodeling.io.xml.stream.StreamBasedXmlDocumentSupplier;
import org.dice_research.topicmodeling.lang.postagging.StandardEnglishPosTaggingTermFilter;
import org.dice_research.topicmodeling.lang.postagging.StanfordPipelineWrapper;
import org.dice_research.topicmodeling.lang.postagging.StopwordlistBasedTermFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentWordCountingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.DocumentFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.ner.NerPropagatingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PropertyRemovingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.TermFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.WordIndexingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.ListCorpusCreator;
import org.dice_research.topicmodeling.utils.corpus.Corpus;
import org.dice_research.topicmodeling.utils.corpus.DocumentListCorpus;
import org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentProperty;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.SimpleVocabulary;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.dice_research.topicmodeling.wikipedia.WikipediaDumpReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class expects an XML corpus and preprocesses it for the topic modeling
 * algorithm. The preprocessed corpus is stored as gzipped object. It is
 * expected to contain a vocabulary. Each document should contain either
 * DocumentTextWordIds or DocumentWordCounts.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class CorpusObjectGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorpusObjectGenerator.class);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tCorpusObjectGenerator <xml-corpus-file> <output-file>");
        }
        File corpusFile = new File(args[0]);
        if (!corpusFile.exists()) {
            throw new IllegalArgumentException("The given corpus file does not exist.");
        }
        File outputFile = new File(args[1]);
        if ((!outputFile.getParentFile().exists()) && (!outputFile.getParentFile().mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }

        CorpusObjectGenerator generator = new CorpusObjectGenerator();
        generator.run(corpusFile, outputFile);
    }

    public void run(File corpusFile, File outputFile) {
        LOGGER.info("Creating pipeline...");
        DocumentSupplier supplier;
        if (corpusFile.toString().contains("wiki")) {
            // Create Wikipedia dump reader
            LOGGER.info("Using Wikipedia dump reader");
            try {
                InputStream input = new BZip2CompressorInputStream(new FileInputStream(corpusFile), true);
                supplier = WikipediaDumpReader.createReader(input, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("Could not create a reader: {}", corpusFile);
                throw new RuntimeException(e);
            }
        } else {
            // Create XML reader
            LOGGER.info("Using XML document supplier");
            supplier = StreamBasedXmlDocumentSupplier.createReader(corpusFile, true);
        }
//        StreamBasedXmlDocumentSupplier.registerParseableDocumentProperty(DatasetClassInfo.class);
//        StreamBasedXmlDocumentSupplier.registerParseableDocumentProperty(DatasetSpecialClassesInfo.class);
//        StreamBasedXmlDocumentSupplier.registerParseableDocumentProperty(DatasetPropertyInfo.class);
//        StreamBasedXmlDocumentSupplier.registerParseableDocumentProperty(DatasetVocabularies.class);

        // Logging of process information
        supplier = new DocumentFilteringSupplierDecorator(supplier, new DocumentFilter() {
            public boolean isDocumentGood(Document document) {
                DocumentName name = document.getProperty(DocumentName.class);
                DocumentURI uri = document.getProperty(DocumentURI.class);
                LOGGER.info("Processing of {} ({}) starts", name != null ? name.get() : "null",
                        uri != null ? uri.get() : "null");
                return true;
            }
        });

        supplier = new NerPropagatingSupplierDecorator(supplier,
                StanfordPipelineWrapper.createStanfordPipelineWrapper(
                PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma"), null));

        // Filter standard stop words
        supplier = new TermFilteringSupplierDecorator(supplier,
                StandardEnglishPosTaggingTermFilter.getInstance());

        // Filter custom stop words
        supplier = new TermFilteringSupplierDecorator(supplier,
                new StopwordlistBasedTermFilter(getClass().getClassLoader().getResourceAsStream("stopwords.txt")));

        // Filter empty documents
        supplier = new DocumentFilteringSupplierDecorator(supplier, new DocumentFilter() {

            public boolean isDocumentGood(Document document) {
                TermTokenizedText text = document.getProperty(TermTokenizedText.class);
                DocumentName name = document.getProperty(DocumentName.class);
                DocumentURI uri = document.getProperty(DocumentURI.class);
                if ((text != null) && (text.getTermTokenizedText().size() > 0)) {
                    LOGGER.info("{} ({}) is accepted as part of the corpus", name != null ? name.get() : "null",
                            uri != null ? uri.get() : "null");
                    return true;
                } else {
                    LOGGER.info("{} ({}) is sorted out and won't be part of the corpus",
                            name != null ? name.get() : "null", uri != null ? uri.get() : "null");
                    return false;
                }
            }
        });

        Vocabulary vocabulary = new SimpleVocabulary();
        supplier = new WordIndexingSupplierDecorator(supplier, vocabulary);
        supplier = new DocumentWordCountingSupplierDecorator(supplier);

        // Since this property is not serializeable we have to remove it
        List<Class<? extends DocumentProperty>> propertiesToRemove = new ArrayList<Class<? extends DocumentProperty>>();
        propertiesToRemove.add(DocumentText.class);
        propertiesToRemove.add(TermTokenizedText.class);
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
