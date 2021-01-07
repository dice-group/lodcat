package org.dice_research.lodcat.preproc;

import java.util.Arrays;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class SimpleUriVerbalizingSupplierDecoratorTest {
    private String input;
    private String[] expected;
    private SimpleUriVerbalizingSupplierDecorator instance;

    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"word", new String[]{"word"}},
            {"one/two", new String[]{"two"}},
            {"data:text/plain,test", null},
            {"http://example.com/", null},
            {"http://example.com/one/two", new String[]{"two"}},
            {"http://example.com/one/two#three", new String[]{"three"}},
            {"http://example.com/one/two#three/four", new String[]{"three/four"}},
            {"http://example.com/test?query", new String[]{"test"}},
        });
    }

    public SimpleUriVerbalizingSupplierDecoratorTest(String input, String[] expected) {
        this.input = input;
        this.expected = expected;
    }

    @Before
    public void setUp() {
        instance = new SimpleUriVerbalizingSupplierDecorator(null);
    }

    @Test
    public void test() {
        assertArrayEquals(expected, instance.verbalizeUri(input));
    }
}
