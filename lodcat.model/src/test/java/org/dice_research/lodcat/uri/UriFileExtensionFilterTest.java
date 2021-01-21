package org.dice_research.lodcat.uri;

import java.util.Arrays;
import org.junit.runners.Parameterized;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.assertEquals;
import org.junit.runner.RunWith;

@RunWith(Parameterized.class)
public class UriFileExtensionFilterTest {
    static UriFilter filter;
    String input;
    boolean expected;

    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"http://example.com/", true},
            {"http://example.com/index.html", true},
            {"http://example.com/is_this_js", true},
            {"http://example.com/directory/", true},
            {"http://example.com/index.css", false},
            {"http://example.com/directory/1.js", false},
            {"data:text/plain,test", true},
        });
    }

    @BeforeClass
    public static void onlyOnce() throws Exception {
        filter = new UriFileExtensionFilter(new String[]{
            "css",
            "js",
        }, true);
    }

    public UriFileExtensionFilterTest(String input, boolean expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertEquals(input, expected, filter.apply(input));
    }
}
