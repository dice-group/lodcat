package org.dice_research.lodcat.preproc;

import java.net.*;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleUriVerbalizingSupplierDecorator extends AbstractUriVerbalizingSupplierDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUriVerbalizingSupplierDecorator.class);

    public SimpleUriVerbalizingSupplierDecorator(DocumentSupplier documentSource) {
        super(documentSource);
    }

    @Override
    protected String[] verbalizeUri(String uri) {
        try {
            URI u = new URI(uri);

            String fragment = u.getFragment();
            if (fragment != null) {
                LOGGER.trace("Verbalizing \"{}\" as \"{}\" (fragment)", uri, fragment);
                return new String[]{fragment};
            }

            String path = u.getPath();
            if (path != null) {
                String[] items = u.getPath().split("/");
                for (int i = items.length - 1; i >= 0; i--) {
                    if (items[i].length() != 0) {
                        LOGGER.trace("Verbalizing \"{}\" as \"{}\" (path)", uri, items[i]);
                        return new String[]{items[i]};
                    }
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Could not parse URI: {}", uri, e);
        }

        LOGGER.trace("No verbalization for \"{}\"", uri);
        return null;
    }
}
