package org.dice_research.lodcat.preproc;

import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.lodcat.uri.UriFilter;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.predicates.ObjectPredicate;

/**
 * Removes all URIs from the {@link UriCounts} property that fail the test with
 * the given {@link UriFilter}.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class UriFilteringSupplierDecorator extends AbstractPropertyEditingDocumentSupplierDecorator<UriCounts> {

    private ObjectPredicate<String> invertedUriFilter;

    public UriFilteringSupplierDecorator(DocumentSupplier documentSource, UriFilter filter) {
        super(documentSource, UriCounts.class);
        // Invert the filter
        invertedUriFilter = new ObjectPredicate<String>() {
            @Override
            public boolean apply(String value) {
                return !filter.test(value);
            }
        };
    }

    @Override
    protected void editDocumentProperty(UriCounts property) {
        ObjectLongOpenHashMap<String> uriCounts = property.get();
        uriCounts.removeAll(invertedUriFilter);
    }
}
