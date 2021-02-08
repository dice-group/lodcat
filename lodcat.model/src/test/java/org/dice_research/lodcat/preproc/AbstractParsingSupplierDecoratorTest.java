package org.dice_research.lodcat.preproc;

import org.dice_research.lodcat.data.UriCounts;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public abstract class AbstractParsingSupplierDecoratorTest {
    String input;
    InputStream stream;
    ObjectLongOpenHashMap<String> expected;
    AbstractParsingSupplierDecorator<UriCounts> parsingDecorator;

    public AbstractParsingSupplierDecoratorTest(String input, ObjectLongOpenHashMap<String> expected) {
        this.input = input;
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        stream = getClass().getClassLoader().getResourceAsStream(input);
    }

    @Test
    public void test() throws Exception {
        assertEquals("Extracted URI counts", expected, parsingDecorator.parseRDF(null, stream).get());
    }
}
