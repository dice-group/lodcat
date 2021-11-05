package org.dice_research.lodcat.preproc;

import java.io.Closeable;

import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the amount of processed documents every Nth document.
 */
public class DocumentCountLoggingSupplierDecorator extends AbstractDocumentSupplierDecorator implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentCountLoggingSupplierDecorator.class);

    private static final long DEFAULT_STEP = 1000;

    private long step;
    private long count = 0;

    public DocumentCountLoggingSupplierDecorator(DocumentSupplier documentSource, long step) {
        super(documentSource);
        this.step = step;
    }

    public DocumentCountLoggingSupplierDecorator(DocumentSupplier documentSource) {
        this(documentSource, DEFAULT_STEP);
    }

    @Override
    protected Document prepareDocument(Document document) {
        if ((++count % step) == 0) {
            LOGGER.info("Documents processed: {}", count);
        }
        return document;
    }

    @Override
    public void close() {
        LOGGER.info("Documents processed: {}", count);
    }
}
