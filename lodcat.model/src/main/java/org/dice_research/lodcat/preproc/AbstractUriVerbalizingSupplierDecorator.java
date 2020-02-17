package org.dice_research.lodcat.preproc;

import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentText;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;

public abstract class AbstractUriVerbalizingSupplierDecorator extends AbstractPropertyAppendingDocumentSupplierDecorator<DocumentText> {

    public AbstractUriVerbalizingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    @Override
    protected DocumentText createPropertyForDocument(Document document) {
        UriCounts uriCounts = document.getProperty(UriCounts.class);
        if (uriCounts == null) {
            throw new IllegalArgumentException(
                    "Got document #" + document.getDocumentId() + " without UriCounts property.");
        }
        return generateText(uriCounts.get());
    }

    protected DocumentText generateText(ObjectLongOpenHashMap<String> uriCounts) {
        StringBuilder builder = new StringBuilder();
        String verbalizations[];
        for (int i = 0; i < uriCounts.allocated.length; ++i) {
            if(uriCounts.allocated[i]) {
                // TODO Should we have a sorting for the verbalizations?
                verbalizations = verbalizeUri(uriCounts.keys[i]);
                // If the URI could be verbalized
                if(verbalizations != null) {
                    int count = (int) Math.floor(Math.log((double) uriCounts.values[i])/Math.log(2));
                    int pos = 0;
                    for (int j = 0; j < count; ++j) {
                        builder.append(verbalizations[pos]);
                        ++pos;
                        if(pos >= verbalizations.length) {
                            pos = 0;
                        }
                    }
                }
            }
        }
        return new DocumentText(builder.toString());
    }

    protected abstract String[] verbalizeUri(String string);

}
