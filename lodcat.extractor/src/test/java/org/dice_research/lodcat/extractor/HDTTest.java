package org.dice_research.lodcat.extractor;

import java.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class HDTTest extends FileTest {
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"simple.hdt", new String[] {"http://example.com/s", "label", "Test"}},
            {"lang.hdt", new String[] {"http://example.com/s", "label", "Test"}},
            {"lang2.hdt", new String[] {"http://example.com/s", "label", "Test"}},
            {"quotes.hdt", new String[] {"http://example.com/s", "label", "1\n\"2\"\n3"}},
            {"blanknode.hdt", new String[] {"http://example.com/s", "label", "Test"}},
        });
    }

    public HDTTest(String input, String[] expecteds) {
        super(input, expecteds);
    }

    @Test
    public void test() throws Exception {
        App.processHDT(file, this::handleExtractedData);
    }
}
