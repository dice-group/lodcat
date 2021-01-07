package org.dice_research.lodcat.preproc;

import java.sql.*;
import java.util.*;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLUriVerbalizingSupplierDecorator extends AbstractUriVerbalizingSupplierDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLUriVerbalizingSupplierDecorator.class);

    private PreparedStatement select;
    private String[] types;

    public SQLUriVerbalizingSupplierDecorator(DocumentSupplier documentSource, String[] types) {
        super(documentSource);

        this.types = types;

        try {
            Connection con = DriverManager.getConnection("jdbc:postgresql://" + System.getenv("DB_HOST") + "/" + System.getenv("DB_DB"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
            select = con.prepareStatement("SELECT value FROM labels WHERE uri=? AND type=?::labelType");
        } catch (SQLException e) {
            LOGGER.error("Error while getting connection");
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String[] verbalizeUri(String uri) {
        List<String> values = new ArrayList<>();
        for (String type : types) {
            try {
                select.setString(1, uri);
                select.setString(2, type);
                ResultSet results = select.executeQuery();
                while (results.next()) {
                    values.add(results.getString("value"));
                }
            } catch (SQLException e) {
                LOGGER.error("Error while executing the query", e);
            }
        }
        LOGGER.trace("Verbalizing \"{}\" as {}", uri, values);

        return values.toArray(new String[]{});
    }

}
