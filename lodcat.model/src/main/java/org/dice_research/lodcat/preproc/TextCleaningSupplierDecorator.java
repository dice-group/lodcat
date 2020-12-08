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

    private static final Pattern NOTHING = Pattern.compile(
            "</?\\w[^>]*>" // HTML tag-lookalikes
            + "|" + "\\W+(?=\\s|$)" // non-word symbols before the whitespace
            + "|" + "(?<=\\s|^)\\W+" // non-word symbols after the whitespace
            + "|" + "[^\\p{ASCII}]" // non-ASCII
            + "|" + "http\\S{3,}" // link-lookalikes
            + "|" + "\\S*@\\S*" // e-mail-lookalikes
    );

    private static final Pattern WORD_SEPARATOR = Pattern.compile(
            "[/_.]"
            + "|" + "(?<=[a-z])(?=[A-Z])" // camelCase
    );

    public TextCleaningSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource, DocumentText.class);
    }

    @Override
    protected void editDocumentProperty(DocumentText docText) {
        String text = docText.getText();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = NOTHING.matcher(text).replaceAll("");
        text = WORD_SEPARATOR.matcher(text).replaceAll(" ");
        text = text.toLowerCase();
        docText.setText(text);
    }
}
