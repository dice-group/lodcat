package org.dice_research.lodcat.extractor;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.jena.graph.impl.LiteralLabel;
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
        Connection con = DriverManager.getConnection("jdbc:postgresql://" + System.getenv("DB_HOST") + "/" + System.getenv("DB_DB"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO labels (uri, type, value, count) VALUES (?, ?::labelType, ?, ?) ON CONFLICT DO NOTHING")) {
            PipedRDFIterator iter = new PipedRDFIterator();
            StreamRDF input = new PipedTriplesStream(iter);
            RDFParser.create().lang(Lang.TURTLE).source(new InputStreamReader(System.in, "UTF8")).build().parse(input);

            int total = 0;
            while (iter.hasNext()) {
                Triple t = (Triple)iter.next();
                String type = types.get(t.getPredicate().getURI());
                if (type != null) {
                    String uri = t.getSubject().getURI();
                    LiteralLabel literal = t.getObject().getLiteral();
                    String language = literal.language();
                    if (language.equals("") || language.equals("en")) {
                        String value = (String) literal.getValue();
                        stmt.setString(1, uri);
                        stmt.setString(2, type);
                        stmt.setString(3, value);
                        stmt.setInt(4, -1); // count
                        stmt.executeUpdate();

                        total += 1;
                    }
                }
            }

            System.out.println(String.format("Total: %d", total));
        }
    }
}
