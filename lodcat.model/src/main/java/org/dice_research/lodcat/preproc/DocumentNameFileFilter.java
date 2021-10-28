package org.dice_research.lodcat.preproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.filter.AbstractDocumentPropertyBasedFilter;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes all documents whose {@link DocumentName} property is listed in the given file.
 */
public class DocumentNameFileFilter extends AbstractDocumentPropertyBasedFilter<DocumentName> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentNameFileFilter.class);
    private Set<String> names = new HashSet<>();

    public DocumentNameFileFilter(File file) {
        super(DocumentName.class);
        LOGGER.info("Reading the name filter list: {}", file);
        try (Stream<String> lines = Files.lines(file.toPath())) {
            lines.forEach(names::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Total names in the filter list: {}", names.size());
    }

    @Override
    protected boolean isDocumentPropertyGood(DocumentName name) {
        return !names.contains(name.get());
    }
}
