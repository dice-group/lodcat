package org.dice_research.lodcat.preproc;

import java.sql.*;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyEditingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextCleaningSupplierDecorator extends AbstractPropertyEditingDocumentSupplierDecorator<DocumentText> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextCleaningSupplierDecorator.class);

    private static final Pattern PATTERN = Pattern.compile(
            "</?\\w[^>]*>"
            + "|" + "\\W+(?=\\s|$)"
            + "|" + "[^\\p{ASCII}]"
    );
    private static final String REPLACEMENT = "";

    public TextCleaningSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource, DocumentText.class);
    }

    @Override
    protected void editDocumentProperty(DocumentText docText) {
        String text = docText.getText();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = PATTERN.matcher(text).replaceAll(REPLACEMENT);
        text = text.toLowerCase();
        docText.setText(text);
    }
}
