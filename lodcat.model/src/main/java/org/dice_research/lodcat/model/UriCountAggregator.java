package org.dice_research.lodcat.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.dice_research.lodcat.data.UriCounts;
import org.dice_research.lodcat.preproc.NameFilteringSupplierDecorator;
import org.dice_research.lodcat.preproc.RDFParsingSupplierDecorator;
import org.dice_research.topicmodeling.io.FolderReader;
import org.dice_research.topicmodeling.io.factories.StreamOpeningFileBasedDocumentFactory;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;
import org.dice_research.topicmodeling.preprocessing.docsupplier.decorator.DocumentFilteringSupplierDecorator;
import org.dice_research.topicmodeling.utils.doc.Document;
import org.dice_research.topicmodeling.utils.doc.DocumentName;
import org.dice_research.topicmodeling.utils.doc.DocumentURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregate some statistics on URI namespaces in a directory with RDF files.
 */
public class UriCountAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriCountAggregator.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "Wrong number of arguments! the following format is expected:\n\tUriCountAggregator <input directory> <output directory> [<file name prefix filter>]");
        }

        UriCountAggregator instance = new UriCountAggregator();
        instance.run(new File(args[0]), new File(args[1]), args.length >= 3 ? args[2] : "");
    }

    public void run(File inputDirectory, File outputDirectory, String filenamePrefix) {
        FolderReader reader = new FolderReader(
            new StreamOpeningFileBasedDocumentFactory(),
            inputDirectory,
                FileFilterUtils.prefixFileFilter(filenamePrefix));

        DocumentSupplier supplier = new DocumentFilteringSupplierDecorator(reader, document -> {
                DocumentName documentName = document.getProperty(DocumentName.class);
                DocumentURI documentURI = document.getProperty(DocumentURI.class);
                LOGGER.trace("Processing: {} ({})...", documentName, documentURI);
                return true;
        });

        supplier = new NameFilteringSupplierDecorator(supplier, docName -> !docName.getName().contains("squirrel_metadata"));

        // Parse the RDF and keep a map of URIs to their counts
        supplier = new RDFParsingSupplierDecorator(supplier);

        LOGGER.info("Processing documents...");
        ObjectLongOpenHashMap<String> totalCounts = new ObjectLongOpenHashMap<>();
        ObjectLongOpenHashMap<String> distinctCounts = new ObjectLongOpenHashMap<>();
        long distinctCountSum = 0;
        Document document;
        while (true) {
            try {
                document = supplier.getNextDocument();

                if (document == null)
                    break;

                DocumentName documentName = document.getProperty(DocumentName.class);
                DocumentURI documentURI = document.getProperty(DocumentURI.class);
                UriCounts uriCounts = document.getProperty(UriCounts.class);

                Set<String> documentNamespaces = new HashSet<>();
                for (ObjectLongCursor cursor : (ObjectLongMap<String>) uriCounts.getValue()) {
                    String namespace = URINamespace((String) cursor.key);
                    totalCounts.putOrAdd(namespace, 1, 1);
                    if (!documentNamespaces.contains(namespace)) {
                        documentNamespaces.add(namespace);
                        distinctCounts.putOrAdd(namespace, 1, 1);
                        distinctCountSum++;
                    }
                }

                LOGGER.trace("Processed: {} ({})", documentName, documentURI);
            } catch (Exception e) {
                LOGGER.error("Exception while processing a document");
            }
        }

        writeMapToFile(totalCounts, new File(outputDirectory, "totalcount-per-namespace.csv"), 2);
        writeMapToFile(distinctCounts, new File(outputDirectory, "documents-per-namespace.csv"), 2);

        ObjectLongOpenHashMap<String> counts = new ObjectLongOpenHashMap<>();
        counts.put("distinctCountsSum", distinctCountSum);
        writeMapToFile(counts, new File(outputDirectory, "counts.csv"), 0);

        LOGGER.info("Done");
    }

    private String URINamespace(String uri) {
        try {
            URI u = new URI(uri);
            if (u.getFragment() != null) {
                return u.resolve("#").toString();
            // } else if (u.getPath().endsWith("/")) {
            //    return u.resolve("..").toString();
            } else {
                return u.resolve(".").toString();
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Exception while parsing an URI: {}", uri);
            return uri;
        }
    }

    private void writeMapToFile(ObjectLongMap<String> map, File file, long minValueToInclude) {
        try (
            OutputStream fos = new FileOutputStream(file);
            Writer osw = new OutputStreamWriter(fos);
            Writer writer = new BufferedWriter(osw)
        ) {
            for (ObjectLongCursor cursor : map) {
                if (cursor.value >= minValueToInclude) {
                    StringBuilder s = new StringBuilder();
                    s.append("\"");
                    s.append(cursor.key);
                    s.append("\", ");
                    s.append(cursor.value);
                    s.append("\n");
                    try {
                        writer.write(s.toString());
                    } catch (IOException e) {
                        LOGGER.error("Exception while writing", e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception while writing", e);
        }
    }
}
