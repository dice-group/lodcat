package org.dice_research.lodcat.preproc;

import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TextCleaningSupplierDecoratorTest {
    private AbstractPropertyEditingDocumentSupplierDecorator<DocumentText> instance;
    private Document doc;

    @Before
    public void setUp() {
        instance = new TextCleaningSupplierDecorator(null);
        doc = new Document();
    }

    @Test
    public void testLowercase() {
        doc.addProperty(new DocumentText("ABC xyz"));
        instance.apply(doc);
        assertEquals("abc xyz", doc.getProperty(DocumentText.class).getText());
    }

    @Test
    public void testPunctuation() {
        doc.addProperty(new DocumentText("abc, def-xyz!"));
        instance.apply(doc);
        assertEquals("abc def-xyz", doc.getProperty(DocumentText.class).getText());
    }

    @Test
    public void testTags() {
        doc.addProperty(new DocumentText("<p>some <a href='http://example.com/'>html tags</a> here<br>"));
        instance.apply(doc);
        assertEquals("some html tags here", doc.getProperty(DocumentText.class).getText());
    }

    @Test
    public void testNormalization() {
        doc.addProperty(new DocumentText("übung café"));
        instance.apply(doc);
        assertEquals("ubung cafe", doc.getProperty(DocumentText.class).getText());
    }

    @Test
    public void testOtherAlphabets() {
        doc.addProperty(new DocumentText("one пример"));
        instance.apply(doc);
        assertEquals("one", doc.getProperty(DocumentText.class).getText());
    }

    @Test
    public void testWordFilter() {
        doc.addProperty(new DocumentText("nice Resource"));
        instance.apply(doc);
        assertEquals("nice ", doc.getProperty(DocumentText.class).getText());
    }
}
