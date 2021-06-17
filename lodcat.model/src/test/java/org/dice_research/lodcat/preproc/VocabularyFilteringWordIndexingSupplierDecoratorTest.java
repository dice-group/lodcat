package org.dice_research.lodcat.preproc;

import static org.junit.Assert.assertEquals;

import java.util.function.Function;

import org.dice_research.topicmodeling.lang.Term;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.TermTokenizedText;
import org.dice_research.topicmodeling.utils.vocabulary.SimpleVocabulary;
import org.dice_research.topicmodeling.utils.vocabulary.Vocabulary;
import org.junit.Before;
import org.junit.Test;

public class VocabularyFilteringWordIndexingSupplierDecoratorTest {
    private String input;
    private String expected;
    private Vocabulary vocabulary;
    private Function<Document, Document> instance;
    private Document doc;

    @Before
    public void setUp() {
        vocabulary = new SimpleVocabulary();
        instance = new VocabularyFilteringWordIndexingSupplierDecorator(null, vocabulary);
        doc = new Document();
    }

    @Test
    public void test() {
        vocabulary.add("word");
        int vocabularySize = vocabulary.size();
        doc.addProperty(new TermTokenizedText(new Term(null, "word"), new Term(null, "nonword")));
        instance.apply(doc);
        assertEquals("Vocabulary size", vocabularySize, vocabulary.size());
        assertEquals("TermTokenizedText", new TermTokenizedText(new Term(null, "word")), doc.getProperty(TermTokenizedText.class));
    }
}
