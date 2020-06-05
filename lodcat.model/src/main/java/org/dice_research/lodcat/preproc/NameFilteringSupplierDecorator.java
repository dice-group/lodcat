package org.dice_research.lodcat.preproc;

import java.util.function.*;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes all documents whose {@link DocumentName} property fails the given test.
 */
public class NameFilteringSupplierDecorator extends AbstractDocumentSupplierDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NameFilteringSupplierDecorator.class);

    private Function<DocumentName,Boolean> test;

    public NameFilteringSupplierDecorator(DocumentSupplier documentSource, Function<DocumentName,Boolean> test) {
        super(documentSource);
        this.test = test;
    }

    @Override
    protected Document prepareDocument(Document document) {
        DocumentName name = document.getProperty(DocumentName.class);
        if (name != null) {
            if (test.apply(name)) {
                return document;
            }
        } else {
            LOGGER.error("Document {} has no DocumentName", document);
        }
        return null;
    }
}
