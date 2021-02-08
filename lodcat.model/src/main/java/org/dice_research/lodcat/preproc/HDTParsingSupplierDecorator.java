package org.dice_research.lodcat.preproc;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import java.io.InputStream;
import java.io.IOException;
import org.apache.commons.compress.utils.IOUtils;
import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentInputStream;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* Parses the {@link DocumentText} as HDT, filters and counts the different URIs
* and returns them as {@link UriCounts} property.
 */
public class HDTParsingSupplierDecorator extends AbstractParsingSupplierDecorator<UriCounts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HDTParsingSupplierDecorator.class);

    public HDTParsingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    @Override
    protected UriCounts createPropertyForDocument(Document document) {
        DocumentInputStream dis = document.getProperty(DocumentInputStream.class);
        if (dis == null) {
            LOGGER.warn("Couldn't get needed DocumentInputStream property from document. Returning null.");
        } else {
            try {
                return parseRDF(document, dis.get());
            } catch (Exception e) {
                LOGGER.error("Got an exception when parsing the content of document " + document.toString()
                        + ". Returning null.", e);
            } finally {
                IOUtils.closeQuietly(dis);
            }
        }
        return null;
    }

    @Override
    protected UriCounts parseRDF(Document document, InputStream is) {
        UriCounter counter = new UriCounter();
        try {
            HDT hdt = HDTManager.loadHDT(is, null);
            try {
                IteratorTripleString iter = hdt.search("", "", "");
                while (iter.hasNext()) {
                    counter.triple(iter.next());
                }
            } catch (NotFoundException e) {
                LOGGER.trace("No triples found for document: {}", document);
            }
            return new UriCounts(counter.getUriCounts());
        } catch (IOException e) {
            LOGGER.error("Cannot load HDT for document: {}", document);
            return null;
        }
    }

    protected class UriCounter {
        private ObjectLongOpenHashMap<String> uriCounts = new ObjectLongOpenHashMap<String>();

        public ObjectLongOpenHashMap<String> getUriCounts() {
            return uriCounts;
        }

        public void triple(TripleString ts) {
            countNode(ts.getSubject());
            countNode(ts.getPredicate());
            countNode(ts.getObject());
        }

        protected void countNode(CharSequence cs) {
            String s = cs.toString();
            if (!s.startsWith("_:") && !s.startsWith("\"")) {
                LOGGER.trace("Incrementing counter for URI: {}", s);
                uriCounts.putOrAdd(s, 1, 1);
            }
        }
    }
}
