package org.dice_research.lodcat.preproc;

import org.dice_research.topicmodeling.lang.postagging.StandardEnglishPosTaggingTermFilter;
import org.dice_research.topicmodeling.lang.postagging.StanfordPipelineWrapper;
import org.dice_research.topicmodeling.lang.postagging.StopwordlistBasedTermFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentWordCountingSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.TermFilteringSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.DocumentFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.PosTaggingSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.util.PropertiesUtils;

/**
 * Handles all preprocessing steps.
 * Expects cleaned text documents as an input.
 */
public class TextProcessingSupplierDecorator implements DocumentSupplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextProcessingSupplierDecorator.class);

    private DocumentSupplier documentSource;
    private DocumentSupplier supplier;

    public TextProcessingSupplierDecorator(DocumentSupplier documentSource, Vocabulary vocabulary) {
        this(documentSource, vocabulary, true);
    }

    public TextProcessingSupplierDecorator(DocumentSupplier documentSource, Vocabulary vocabulary, boolean addNewWordsToVocabulary) {
        this.documentSource = documentSource;
        supplier = documentSource;

        // Filter documents without text property (due to source files failing to parse when the corpus was built)
        supplier = new DocumentFilteringSupplierDecorator(supplier, new DocumentFilter() {
            public boolean isDocumentGood(Document document) {
                return document.getProperty(DocumentText.class) != null;
            }
        });

        // Tokenize the text
        supplier = new PosTaggingSupplierDecorator(
                supplier,
                new ChunkingPosTaggerDecorator(
                    StanfordPipelineWrapper.createStanfordPipelineWrapper(
                        PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma"),
                        null
		    )
		)
	);

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
        supplier = new TermFilteringSupplierDecorator(supplier,
                StandardEnglishPosTaggingTermFilter.getInstance());

        // Filter custom stop words
        supplier = new TermFilteringSupplierDecorator(supplier,
                new StopwordlistBasedTermFilter(getClass().getClassLoader().getResourceAsStream("stopwords.txt")));

        // Remove special non-word tokens
        supplier = new TermFilteringSupplierDecorator(supplier, term -> !(term.getPosTag().startsWith("-") && term.getPosTag().endsWith("-")));

        if (addNewWordsToVocabulary) {
            supplier = new VocabularyAddingWordIndexingSupplierDecorator(supplier, vocabulary);
        } else {
            supplier = new VocabularyFilteringWordIndexingSupplierDecorator(supplier, vocabulary);
        }

        supplier = new DocumentWordCountingSupplierDecorator(supplier);
    }

    @Override
    public Document getNextDocument() {
        return supplier.getNextDocument();
    }

    @Override
    public void setDocumentStartId(int documentStartId) {
        documentSource.setDocumentStartId(documentStartId);
    }
}
