package org.dice_research.lodcat.preproc;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.AbstractPropertyAppendingDocumentSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds the URI of the graph that has been crawled to gather the content of a
 * given document. It utilizes the file name of the document and the metadata
 * graph generated by the Squirrel Linked Data crawler to retrieve the URI.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SquirrelMetadataAddingSupplierDecorator
        extends AbstractPropertyAppendingDocumentSupplierDecorator<DocumentURI> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SquirrelMetadataAddingSupplierDecorator.class);

    private Map<String, String> fileToUriMapping;

    public static SquirrelMetadataAddingSupplierDecorator create(DocumentSupplier documentSource,
            File squirrelMetaData) {
        Map<String, String> fileToUriMapping = new HashMap<>();
        readFileToUriMapping(squirrelMetaData, Collections.synchronizedMap(fileToUriMapping));
        return new SquirrelMetadataAddingSupplierDecorator(documentSource, fileToUriMapping);
    }

    private static void readFileToUriMapping(File squirrelMetaData, Map<String, String> fileToUriMapping) {
        if (squirrelMetaData.isDirectory()) {
            Arrays.stream(squirrelMetaData.listFiles()).parallel()
                    .forEach(f -> readFileToUriMapping(f, fileToUriMapping));
        } else {
            if (squirrelMetaData.isFile()) {
                RDFDataMgr.parse(new SquirrelFileUriMapper(fileToUriMapping),
                        "file://" + squirrelMetaData.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("Given file \"" + squirrelMetaData.toString()
                        + "\" is neither a directory nor a file. Aborting.");
            }
        }
    }

    public SquirrelMetadataAddingSupplierDecorator(DocumentSupplier documentSource,
            Map<String, String> fileToUriMapping) {
        super(documentSource);
        this.fileToUriMapping = fileToUriMapping;
    }

    @Override
    protected DocumentURI createPropertyForDocument(Document document) {
        DocumentName name = document.getProperty(DocumentName.class);
        if (name != null) {
            String fileName = "file:///var/squirrel/data/" + name.get();
            if (fileToUriMapping.containsKey(fileName)) {
                return new DocumentURI(fileToUriMapping.get(fileName));
            } else {
                LOGGER.error(
                        "Got a document file name \"{}\" that couldn't be found in the metadata map. Returning null.",
                        fileName);
            }
        } else {
            LOGGER.error("Couldn't get the name of document #{}. Returning null.", document.getDocumentId());
        }
        return null;
    }

    /**
     * Apache Jena StreamRDF implementation collecting the triples of the
     * {@link #CONTAINS_DATA_OF} property.
     * 
     * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
     *
     */
    protected static class SquirrelFileUriMapper extends StreamRDFBase implements StreamRDF {

        public static final Node CONTAINS_DATA_OF = ResourceFactory
                .createProperty("http://w3id.org/squirrel/vocab#containsDataOf").asNode();

        private Map<String, String> fileToUriMapping;

        public SquirrelFileUriMapper(Map<String, String> fileToUriMapping) {
            this.fileToUriMapping = fileToUriMapping;
        }

        @Override
        public void triple(Triple triple) {
            if (triple.predicateMatches(CONTAINS_DATA_OF)) {
                fileToUriMapping.put(triple.getSubject().getURI(), triple.getObject().getURI());
            }
        }

        @Override
        public void quad(Quad quad) {
            triple(quad.asTriple());
        }
    }
}
