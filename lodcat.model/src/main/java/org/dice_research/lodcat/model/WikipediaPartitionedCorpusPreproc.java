package org.dice_research.lodcat.model;

import java.io.File;
import java.io.IOException;

import org.dice_research.topicmodeling.io.xml.XmlBasedCorpusPartWriter;
import org.dice_research.topicmodeling.io.xml.XmlWritingDocumentConsumer;
import org.dice_research.topicmodeling.io.xml.stream.StreamBasedXmlDocumentSupplier;
import org.dice_research.topicmodeling.lang.postagging.StandardEnglishPosTaggingTermFilter;
import org.dice_research.topicmodeling.lang.postagging.StanfordPipelineWrapper;
import org.dice_research.topicmodeling.lang.postagging.StopwordlistBasedTermFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentTextWithTermInfoCreatingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.TermFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.DocumentFilter;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.util.PropertiesUtils;

/**
 * Splits a single XML Wikipedia file to multiple XML corpus files.
 */
public class WikipediaPartitionedCorpusPreproc implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaPartitionedCorpusPreproc.class);

    private File inputDirectory;
    private File outputDirectory;
    private int startPartId;
    private int endPartId;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tWikipediaPartitionedCorpusPreproc <wikipedia-xml-file> <output-dir> <start-part-id-(inclusive)> <end-part-id-(inclusive)>");
        }
        File inputCorpusFile = new File(args[0]);
        if (!inputCorpusFile.exists()) {
            throw new IllegalArgumentException("The given wikipedia file does not exist.");
        }
        File outputDirectory = new File(args[1]);
        if ((!outputDirectory.exists()) && (!outputDirectory.mkdirs())) {
            throw new IllegalArgumentException("Couldn't create output directory.");
        }
        int startPartId = Integer.parseInt(args[2]);
        int endPartId = Integer.parseInt(args[3]);

        (new WikipediaPartitionedCorpusPreproc(inputCorpusFile, outputDirectory, startPartId, endPartId)).run();
    }

    public WikipediaPartitionedCorpusPreproc(File inputDirectory, File outputDirectory, int startPartId,
            int endPartId) {
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.startPartId = startPartId;
        this.endPartId = endPartId;
    }

    @Override
    public void run() {
        for (int i = startPartId; i < endPartId; ++i) {
            try {
                processPart(i);
            } catch (IOException e) {
                LOGGER.error("Error while processing part {}", i);
            }
        }
    }

    public void processPart(int partId) throws IOException {
        DocumentSupplier supplier;
        File partFile = new File(outputDirectory.getAbsolutePath() + File.separator
                + XmlBasedCorpusPartWriter.PART_FILE_PREFIX + partId + XmlBasedCorpusPartWriter.PART_FILE_SUFFIX);
        if (!partFile.exists()) {
            LOGGER.info("Skipping missing part {}", partId);
            return;
        }
        XmlWritingDocumentConsumer writer = XmlWritingDocumentConsumer.createXmlWritingDocumentConsumer(
                new File(outputDirectory.getAbsolutePath() + File.separator + XmlBasedCorpusPartWriter.PART_FILE_PREFIX
                        + partId + XmlBasedCorpusPartWriter.PART_FILE_SUFFIX));
        try {
            supplier = StreamBasedXmlDocumentSupplier.createReader(inputDirectory + File.separator
                    + XmlBasedCorpusPartWriter.PART_FILE_PREFIX + partId + XmlBasedCorpusPartWriter.PART_FILE_SUFFIX);

            // START COPIED FROM TextProcessingSupplierDecorator

            // Filter documents without text property (due to source files failing to parse
            // when the corpus was built)
            supplier = new DocumentFilteringSupplierDecorator(supplier, new DocumentFilter() {
                public boolean isDocumentGood(Document document) {
                    return document.getProperty(DocumentText.class) != null;
                }
            });

            // Tokenize the text
            supplier = new PosTaggingSupplierDecorator(supplier, StanfordPipelineWrapper.createStanfordPipelineWrapper(
                    PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma"), null));

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

            // Filter standard stop words
            supplier = new TermFilteringSupplierDecorator(supplier, StandardEnglishPosTaggingTermFilter.getInstance());

            // Filter custom stop words
            supplier = new TermFilteringSupplierDecorator(supplier,
                    new StopwordlistBasedTermFilter(getClass().getClassLoader().getResourceAsStream("stopwords.txt")));

            // Remove special non-word tokens
            supplier = new TermFilteringSupplierDecorator(supplier,
                    term -> !(term.getPosTag().startsWith("-") && term.getPosTag().endsWith("-")));
            // END COPIED FROM TextProcessingSupplierDecorator

            // Replace text with term tokenized text information
            supplier = new DocumentTextWithTermInfoCreatingSupplierDecorator(supplier);
            
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
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.error("Exception while closing writer.", e);
                }
            }
        }
    }

}
