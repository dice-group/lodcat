package org.dice_research.lodcat.extractor;

import java.io.FileInputStream;
import java.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TTLTest extends FileTest {
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"simple.ttl", new String[] {"http://example.com/s", "label", "Test"}},
            {"lang.ttl", new String[] {"http://example.com/s", "label", "Test"}},
            {"lang2.ttl", new String[] {"http://example.com/s", "label", "Test"}},
            {"quotes.ttl", new String[] {"http://example.com/s", "label", "1\n\"2\"\n3"}},
            {"blanknode.ttl", new String[] {"http://example.com/s", "label", "Test"}},
        });
    }

    public TTLTest(String input, String[] expecteds) {
        super(input, expecteds);
    }

    @Test
    public void test() throws Exception {
        App.processTTL(new FileInputStream(file), this::handleExtractedData);
    }
}
