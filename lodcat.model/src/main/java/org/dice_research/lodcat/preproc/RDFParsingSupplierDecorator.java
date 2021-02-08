package org.dice_research.lodcat.preproc;

import java.io.BufferedInputStream;
import org.apache.jena.riot.Lang;
import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.rdfdetector.RdfSerializationDetector;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentInputStream;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the {@link DocumentText} as RDF, filters and counts the different URIs
 * and returns them as {@link UriCounts} property.
 */
public class RDFParsingSupplierDecorator extends AbstractPropertyAppendingDocumentSupplierDecorator<UriCounts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RDFParsingSupplierDecorator.class);

    private static final JenaBasedParsingSupplierDecorator jenaBased = new JenaBasedParsingSupplierDecorator(null);
    private static final HDTParsingSupplierDecorator hdt = new HDTParsingSupplierDecorator(null);

    public RDFParsingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    @Override
    protected UriCounts createPropertyForDocument(Document document) {
        DocumentInputStream dis = document.getProperty(DocumentInputStream.class);
        BufferedInputStream buffered = new BufferedInputStream(dis.get());
        document.addProperty(new DocumentInputStream(buffered));
        RdfSerializationDetector detector = new RdfSerializationDetector(4);
        for (Lang lang : detector.detect(buffered)) {
            if (lang == RdfSerializationDetector.HDT()) {
                return hdt.createPropertyForDocument(document);
            } else {
                return jenaBased.createPropertyForDocument(document);
            }
        }
        return null;
    }
}
