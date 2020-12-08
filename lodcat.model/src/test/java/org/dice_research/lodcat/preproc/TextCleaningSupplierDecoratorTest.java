package org.dice_research.lodcat.preproc;

import java.util.Arrays;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TextCleaningSupplierDecoratorTest {
    private String input;
    private String expected;
    private AbstractPropertyEditingDocumentSupplierDecorator<DocumentText> instance;
    private Document doc;

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"ABC xyz", "abc xyz"},
            {"abc, def-xyz!", "abc def-xyz"},
            {"<p>some <a href='http://example.com/'>html tags</a> here<br>", "some html tags here"},
            {"übung café", "ubung cafe"},
            {"one пример", "one"},
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
    public void test() {
        doc.addProperty(new DocumentText(input));
        instance.apply(doc);
        assertEquals(expected, doc.getProperty(DocumentText.class).getText());
    }
}
