package org.dice_research.lodcat.extractor;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.vocabulary.*;
import org.dice_research.rdfdetector.RdfSerializationDetector;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts labels and descriptions from Turtle input into a SQL database.
 *
 */
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

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

    private static final Pattern LANG = Pattern.compile("en(?:-.*)?");
    private static final Pattern HDT_LITERAL = Pattern.compile("\"(.*)\"(?:@" + LANG.pattern() + ")?", Pattern.DOTALL);

    private static final String stringDatatype = XSD.xstring.getURI();
    private static final String langStringDatatype = RDF.langString.getURI();

    private static PreparedStatement stmt;

    static {
        for (String uri : NAMING_PROPERTIES) {
            types.put(uri, "label");
        }
        types.put(RDFS.comment.getURI(), "description");
    }

    public static void main(String[] args) throws Exception {
        assert args.length >= 1 : "missing RDF file name";
        String rdfFileName = args[0];

        String conStr = "jdbc:postgresql://" + System.getenv("DB_HOST") + "/" + System.getenv("DB_DB");
        Connection con = DriverManager.getConnection(conStr, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

        stmt = con.prepareStatement("INSERT INTO labels (uri, type, value, count) VALUES (?, ?::labelType, ?, ?) ON CONFLICT DO NOTHING");

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(new File(rdfFileName)));
        RdfSerializationDetector detector = new RdfSerializationDetector(4);
        for (Lang lang : detector.detect(in)) {
            if (lang == RdfSerializationDetector.HDT()) {
                processHDT(rdfFileName, App::insertIntoDatabase);
            } else {
                processTTL(in, App::insertIntoDatabase);
            }
            break;
        }
    }

    static void processHDT(String rdfFileName, Consumer<String[]> handler) throws Exception {
        LOGGER.debug("Processing HDT file: {}", rdfFileName);

        HDT hdt = HDTManager.loadHDT(rdfFileName, null);
        int extracted = 0;
        int total = 0;
        for (Map.Entry<String, String> type : types.entrySet()) {
            IteratorTripleString iter = hdt.search("", type.getKey(), "");
            while (iter.hasNext()) {
                TripleString ts = iter.next();
                LOGGER.trace("Triple: {}", ts);
                String s = ts.getSubject().toString();
                if (!s.startsWith("_:")) {
                    String o = ts.getObject().toString();
                    Matcher m = HDT_LITERAL.matcher(o);
                    if (m.matches()) {
                        String[] args = new String[] {
                            s,
                            type.getValue(),
                            m.group(1),
                        };
                        LOGGER.trace("Extracted: {}", (Object)args);
                        handler.accept(args);
                        extracted += 1;
                    } else {
                        LOGGER.trace("Ignored");
                    }
                } else {
                    LOGGER.trace("Subject is a blank node");
                }
                total += 1;
            }
        }
        LOGGER.info("Found {} triples, extracted {} labels", total, extracted);
    }

    static void processTTL(InputStream in, Consumer<String[]> handler) throws Exception {
        LOGGER.debug("Processing turtle stream: {}", in);

        PipedRDFIterator iter = new PipedRDFIterator();
        StreamRDF streamRDF = new PipedTriplesStream(iter);
        Reader reader = new BufferedReader(new InputStreamReader(in, "UTF8"));

        // Typically, data is read from a PipedRDFIterator by one thread (the consumer) and data is written to the corresponding PipedRDFStream by some other thread (the producer).
        // Attempting to use both objects from a single thread is not recommended, as it may deadlock the thread.
        // https://jena.apache.org/documentation/javadoc/arq/org/apache/jena/riot/lang/PipedRDFIterator.html
        new Thread(new Runnable() {
            public void run() {
                try {
                    RDFParser.create().lang(Lang.TURTLE).source(reader).build().parse(streamRDF);
                } catch (org.apache.jena.riot.RiotException e) {
                    LOGGER.error("Exception in the parser thread", e);
                }
            }
        }).start();

        int extracted = 0;
        int total = 0;
        while (iter.hasNext()) {
            Triple t = (Triple)iter.next();
            LOGGER.trace("Triple: {}", t);
            String type = types.get(t.getPredicate().getURI());
            if (type != null) {
                Node subject = t.getSubject();
                Node object = t.getObject();
                if (subject.isURI()) {
                    if (object.isLiteral()) {
                        LiteralLabel literal = object.getLiteral();
                        String datatypeURI = literal.getDatatypeURI();
                        if (datatypeURI == null || datatypeURI.equals(stringDatatype) || datatypeURI.equals(langStringDatatype)) {
                            String language = literal.language();
                            if (language.equals("") || LANG.matcher(language).matches()) {
                                String[] args = new String[] {
                                    subject.getURI(),
                                    type,
                                    (String) literal.getValue(),
                                };
                                LOGGER.trace("Extracted: {}", (Object)args);
                                handler.accept(args);
                                extracted += 1;
                            } else {
                                LOGGER.trace("Ignored due to language");
                            }
                        }
                    } else {
                        LOGGER.trace("Object is not a literal");
                    }
                } else {
                    LOGGER.trace("Subject is not a URI");
                }
            }
            total += 1;
        }

        LOGGER.info("Found {} triples, extracted {} labels", total, extracted);
    }

    private static void insertIntoDatabase(String[] args) {
        try {
            stmt.setString(1, args[0]); // uri
            stmt.setString(2, args[1]); // type
            stmt.setString(3, args[2]); // value
            stmt.setInt(4, -1); // count
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
