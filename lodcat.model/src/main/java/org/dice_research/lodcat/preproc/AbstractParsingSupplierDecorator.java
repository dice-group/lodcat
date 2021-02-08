package org.dice_research.lodcat.preproc;

import java.io.InputStream;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentProperty;
import org.dice_research.topicmodeling.utils.doc.DocumentText;

/**
* Parses the {@link DocumentText}, extracting a property of type T.
 */
public abstract class AbstractParsingSupplierDecorator<T extends DocumentProperty> extends AbstractPropertyAppendingDocumentSupplierDecorator<T> {
    public AbstractParsingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    protected abstract T parseRDF(Document document, InputStream is);
}
