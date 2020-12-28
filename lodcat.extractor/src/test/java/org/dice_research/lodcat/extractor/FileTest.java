package org.dice_research.lodcat.extractor;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class FileTest {
    String input;
    String file;
    String[] expecteds;
    int amount = 0;

    public FileTest(String input, String[] expecteds) {
        this.input = input;
        this.expecteds = expecteds;
    }

    @Before
    public void setUp() throws Exception {
        file = new File(getClass().getClassLoader().getResource(input).toURI()).toString();
    }

    @After
    public void tearDown() {
        assertEquals("Number of triples extracted", 1, amount);
    }

    public void handleExtractedData(String[] actuals) {
        assertArrayEquals("Extracted triple", expecteds, actuals);
        amount++;
    }
}
