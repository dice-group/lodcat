package org.dice_research.lodcat.preproc;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import java.util.Arrays;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HDTParsingSupplierDecoratorTest extends AbstractParsingSupplierDecoratorTest {
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"counts.hdt", ObjectLongOpenHashMap.from(
                new String[]{"http://example.com/a", "http://example.com/b", "http://example.com/c"},
                new long[]{1, 2, 3})},
        });
    }

    public HDTParsingSupplierDecoratorTest(String input, ObjectLongOpenHashMap<String> expected) {
        super(input, expected);
        parsingDecorator = new HDTParsingSupplierDecorator(null);
    }
}
