package org.dice_research.lodcat.io;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dice_research.topicmodeling.io.CSVFileProcessor;
import org.dice_research.topicmodeling.preprocessing.docconsumer.DocumentConsumer;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentProperty;
import org.dice_research.topicmodeling.utils.doc.StringContainingDocumentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

public class CSVWritingDocumentConsumer implements DocumentConsumer, Closeable, CSVFileProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVWritingDocumentConsumer.class);

    protected CSVWriter fout;
    protected List<Class<? extends DocumentProperty>> fields;

    public static CSVWritingDocumentConsumer createCSVWritingDocumentConsumer(File file, List<Class<? extends DocumentProperty>> fields) {
        CSVWriter writer = null;
        // We need an absolute path, otherwise we might not be able to ask for the
        // parent file
        if (!file.isAbsolute()) {
            file = file.getAbsoluteFile();
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            writer = new CSVWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)),
                                                          StandardCharsets.UTF_8), SEPARATOR, QUOTECHAR, ESCAPECHAR);
            CSVWritingDocumentConsumer consumer = new CSVWritingDocumentConsumer(writer, fields);
            return consumer;
        } catch (Exception e) {
            LOGGER.error("Error while trying to write corpus to {}. Returning null.", file, e);
            IOUtils.closeQuietly(writer);
        }
        return null;
    }

    private CSVWritingDocumentConsumer(CSVWriter fout, List<Class<? extends DocumentProperty>> fields) {
        this.fout = fout;
        this.fields = fields;
    }

    @Override
    public void consumeDocument(Document document) {
        String line[] = new String[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            Class<? extends DocumentProperty> propertyClass = fields.get(i);
            DocumentProperty property = document.getProperty(propertyClass);
            if (property != null ) {
                if (StringContainingDocumentProperty.class.isAssignableFrom(propertyClass)) {
                    line[i] = ((StringContainingDocumentProperty) property).getStringValue();
                } else {
                    line[i] = property.getValue().toString();
                }
            }
        }
        fout.writeNext(line);
    }

    public void close() throws IOException {
        if (fout != null) {
            fout.close();
            fout = null;
        }
    }
}
