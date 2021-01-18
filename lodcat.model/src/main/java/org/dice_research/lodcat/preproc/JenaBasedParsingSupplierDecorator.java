/**
 * tapioca.core - ${project.description}
 * Copyright © 2015 Data Science Group (DICE) (michael.roeder@uni-paderborn.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * This file is part of tapioca.core.
 *
 * tapioca.core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * tapioca.core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with tapioca.core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dice_research.lodcat.preproc;

import java.io.InputStream;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentInputStream;
import org.dice_research.topicmodeling.utils.doc.DocumentText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;

/**
 * Parses the {@link DocumentText} as RDF VOID information (in turtle), filters
 * and counts the different URIs and returns them as {@link UriCounts} property.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class JenaBasedParsingSupplierDecorator extends AbstractPropertyAppendingDocumentSupplierDecorator<UriCounts> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JenaBasedParsingSupplierDecorator.class);

    public JenaBasedParsingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    @Override
    protected UriCounts createPropertyForDocument(Document document) {
//        DocumentText text = document.getProperty(DocumentText.class);
//        if (text == null) {
//            LOGGER.warn("Couldn't get needed DocumentText property from document. Returning null.");
//            return null;
//        } else {
//            return parseRDF(document, text.getText());
//        }
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

    private UriCounts parseRDF(Document document, InputStream is) {
//        UriCounter counter = new UriCounter();
//        RDFParser parser = RDFParser.create().base("").fromString(text).lang(Lang.TTL).build();
//        try {
//            parser.parse(counter);
//            return new UriCounts(counter.getUriCounts());
//        } catch (Exception e) {
//            LOGGER.error("Got an exception when parsing the content of document " + document.toString()
//                    + ". Returning null.", e);
//        }
//        return null;
        UriCounter counter = new UriCounter();
        RDFParser parser = RDFParser.create().base("").source(is).lang(Lang.TTL).build();
        parser.parse(counter);
        return new UriCounts(counter.getUriCounts());
    }

    protected class UriCounter implements StreamRDF {
        private ObjectLongOpenHashMap<String> uriCounts = new ObjectLongOpenHashMap<String>();

        public ObjectLongOpenHashMap<String> getUriCounts() {
            return uriCounts;
        }

        @Override
        public void triple(Triple triple) {
            try {
                countNode(triple.getSubject());
                countNode(triple.getPredicate());
                countNode(triple.getObject());
            } catch (Exception e) {
                LOGGER.error("Couldn't parse the triple \"" + triple + "\".", e);
            }
        }

        protected void countNode(Node n) {
            if (n.isURI()) {
                uriCounts.putOrAdd(n.getURI(), 1, 1);
            }
        }

        @Override
        public void base(String arg0) {
            // nothing to do
        }

        @Override
        public void finish() {
            // nothing to do
        }

        @Override
        public void prefix(String arg0, String arg1) {
            // nothing to do
        }

        @Override
        public void quad(Quad quad) {
            triple(quad.asTriple());
        }

        @Override
        public void start() {
            // nothing to do
        }
    }
}
