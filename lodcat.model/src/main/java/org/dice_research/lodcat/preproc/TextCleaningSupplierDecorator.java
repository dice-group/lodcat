package org.dice_research.lodcat.preproc;

import java.sql.*;
import java.util.*;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextCleaningSupplierDecorator extends AbstractPropertyEditingDocumentSupplierDecorator<DocumentText> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextCleaningSupplierDecorator.class);

    private static final Pattern PATTERN = Pattern.compile("\\W+(?=\\s|$)");
    private static final String REPLACEMENT = "";

    public TextCleaningSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource, DocumentText.class);
    }

    @Override
    protected void editDocumentProperty(DocumentText docText) {
        String text = docText.getText();
        text = PATTERN.matcher(text).replaceAll(REPLACEMENT);
        docText.setText(text);
    }
}
