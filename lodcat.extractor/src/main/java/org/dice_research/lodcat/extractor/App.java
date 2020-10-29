package org.dice_research.lodcat.extractor;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;

/**
 * Extracts labels and descriptions from Turtle input into a SQL database.
 *
 */
public class App {
    static Map<String, String> types = Map.of(
        "http://www.w3.org/2000/01/rdf-schema#label", "label",
        "http://www.w3.org/2000/01/rdf-schema#comment", "description");

    public static void main(String[] args) throws Exception {
        String conStr = "jdbc:postgresql://" + System.getenv("DB_HOST") + "/" + System.getenv("DB_DB");
        Connection con = DriverManager.getConnection(conStr, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO labels (uri, type, value, count) VALUES (?, ?::labelType, ?, ?) ON CONFLICT DO NOTHING")) {
            PipedRDFIterator iter = new PipedRDFIterator();
            StreamRDF streamRDF = new PipedTriplesStream(iter);
            Reader reader = new BufferedReader(new InputStreamReader(System.in, "UTF8"));

            // Typically, data is read from a PipedRDFIterator by one thread (the consumer) and data is written to the corresponding PipedRDFStream by some other thread (the producer).
            // Attempting to use both objects from a single thread is not recommended, as it may deadlock the thread.
            // https://jena.apache.org/documentation/javadoc/arq/org/apache/jena/riot/lang/PipedRDFIterator.html
            new Thread(new Runnable() {
                public void run() {
                    RDFParser.create().lang(Lang.TURTLE).source(reader).build().parse(streamRDF);
                }
            }).start();

            int extracted = 0;
            int total = 0;
            while (iter.hasNext()) {
                Triple t = (Triple)iter.next();
                String type = types.get(t.getPredicate().getURI());
                if (type != null) {
                    Node subject = t.getSubject();
                    Node object = t.getObject();
                    if (subject.isURI() && object.isLiteral()) {
                        LiteralLabel literal = object.getLiteral();
                        String datatypeURI = literal.getDatatypeURI();
                        if (datatypeURI == null || datatypeURI.equals("http://www.w3.org/2001/XMLSchema#string") || datatypeURI.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")) {
                            String language = literal.language();
                            if (language.equals("") || language.equals("en")) {
                                String uri = subject.getURI();
                                String value = (String) literal.getValue();
                                stmt.setString(1, uri);
                                stmt.setString(2, type);
                                stmt.setString(3, value);
                                stmt.setInt(4, -1); // count
                                stmt.executeUpdate();

                                extracted += 1;
                            }
                        }
                    }
                }
                total += 1;
            }

            System.err.println(String.format("Extracted %d/%d", extracted, total));
        }
    }
}
