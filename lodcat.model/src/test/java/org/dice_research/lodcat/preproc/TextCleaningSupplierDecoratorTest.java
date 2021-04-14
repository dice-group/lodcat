package org.dice_research.lodcat.preproc;

import java.util.Arrays;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import static org.dice_research.lodcat.preproc.TextCleaningSupplierDecorator.normalizeNFD;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TextCleaningSupplierDecoratorTest {
    private String input;
    private String expected;
    private AbstractPropertyEditingDocumentSupplierDecorator<DocumentText> instance;
    private Document doc;

    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"ABC xyz", "abc xyz"},
            {"abc, def-xyz!", "abc def-xyz"},
            {"<p>some <a href='http://example.com/'>html tags</a> here<br>", "some html tags here"},
            {"übung café", "ubung cafe"},
            {"one пример", "one"},
            {"under_score sl/ash do.t", "under score sl ash do t"},
            {"#sharp it's 'nothing", "sharp it's nothing"},
            {"some very.nice@address.at.local", "some "},
            {"some camelCaseWords AndCapitalizedWords", "some camel case words and capitalized words"},
            {"follow http://example.com/test/index.html#abc this", "follow  this"},
        });
    }

    public TextCleaningSupplierDecoratorTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Before
    public void setUp() {
        instance = new TextCleaningSupplierDecorator(null);
        doc = new Document();
    }

    @Test
    public void testChunkedNormalization() {
        assertEquals("Text normalized with small chunks", normalizeNFD(input), normalizeNFD(input, 1));
    }

    @Test
    public void testCleanedText() {
        doc.addProperty(new DocumentText(input));
        instance.apply(doc);
        assertEquals("Cleaned text", expected, doc.getProperty(DocumentText.class).getText());
    }
}
