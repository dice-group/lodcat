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
    // https://github.com/dice-group/Tapioca/blob/d8dfef11575114408b62cc8a72dfb4e50702810e/tapioca.core/src/main/java/org/aksw/simba/tapioca/preprocessing/labelretrieving/RDFClientLabelRetriever.java#L42-L77
    private static final String NAMING_PROPERTIES[] = { "http://www.w3.org/2000/01/rdf-schema#label",
            "http://xmlns.com/foaf/0.1/nick",
            "http://purl.org/dc/elements/1.1/title",
            "http://purl.org/rss/1.0/title",
            "http://xmlns.com/foaf/0.1/name",
            "http://purl.org/dc/terms/title",
            "http://www.geonames.org/ontology#name",
            "http://xmlns.com/foaf/0.1/nickname",
            "http://swrc.ontoware.org/ontology#name",
            "http://sw.cyc.com/CycAnnotations_v1#label",
            "http://rdf.opiumfield.com/lastfm/spec#title",
            "http://www.proteinontology.info/po.owl#ResidueName",
            "http://www.proteinontology.info/po.owl#Atom",
            "http://www.proteinontology.info/po.owl#Element",
            "http://www.proteinontology.info/po.owl#AtomName",
            "http://www.proteinontology.info/po.owl#ChainName",
            "http://purl.uniprot.org/core/fullName",
            "http://purl.uniprot.org/core/title",
            "http://www.aktors.org/ontology/portal#has-title",
            "http://www.w3.org/2004/02/skos/core#prefLabel",
            "http://www.aktors.org/ontology/portal#name",
            "http://xmlns.com/foaf/0.1/givenName",
            "http://www.w3.org/2000/10/swap/pim/contact#fullName",
            "http://xmlns.com/foaf/0.1/surName",
            "http://swrc.ontoware.org/ontology#title",
            "http://swrc.ontoware.org/ontology#booktitle",
            "http://www.aktors.org/ontology/portal#has-pretty-name",
            "http://purl.uniprot.org/core/orfName",
            "http://purl.uniprot.org/core/name",
            "http://www.daml.org/2003/02/fips55/fips-55-ont#name",
            "http://www.geonames.org/ontology#alternateName",
            "http://purl.uniprot.org/core/locusName",
            "http://www.w3.org/2004/02/skos/core#altLabel",
            "http://creativecommons.org/ns#attributionName",
            "http://www.aktors.org/ontology/portal#family-name",
            "http://www.aktors.org/ontology/portal#full-name" };

    private static Map<String, String> types = new HashMap<>();

    static {
        for (String uri : NAMING_PROPERTIES) {
            types.put(uri, "label");
        }
        types.put("http://www.w3.org/2000/01/rdf-schema#comment", "description");
    }

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
                    try {
                        RDFParser.create().lang(Lang.TURTLE).source(reader).build().parse(streamRDF);
                    } catch (org.apache.jena.riot.RiotException e) {
                        System.err.println("Exception in the parser thread: " + e);
                    }
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
