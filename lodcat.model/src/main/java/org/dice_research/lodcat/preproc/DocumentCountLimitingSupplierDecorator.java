package org.dice_research.lodcat.preproc;

import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops processing after the specified amount of documents is processed.
 */
public class DocumentCountLimitingSupplierDecorator extends AbstractDocumentSupplierDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentCountLimitingSupplierDecorator.class);

    private long count = 0;
    private long limit = 0;

    public DocumentCountLimitingSupplierDecorator(DocumentSupplier documentSource, long limit) {
        super(documentSource);
        this.limit = limit;
        LOGGER.info("Document limit: {}", limit);
    }

    @Override
    protected Document prepareDocument(Document document) {
        if (++count > limit) {
            LOGGER.warn("Document limit reached, other documents will not be processed: {}", limit);
            // Since null is returned instead of a Document, this stops all further processing!
            return null;
        }
        return document;
    }
}
