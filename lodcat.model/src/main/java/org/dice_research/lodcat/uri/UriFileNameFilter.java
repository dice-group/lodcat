package org.dice_research.lodcat.uri;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter based on a list of given file names and a flag indicating
 * whether it is a blacklist of whitelist filter.
 */
public class UriFileNameFilter implements UriFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriFileNameFilter.class);

    private boolean blacklist;
    private String[] names;

    public UriFileNameFilter(String[] names) {
        this(names, true);
    }

    public UriFileNameFilter(String[] names, boolean blacklist) {
        this.blacklist = blacklist;
        this.names = names;
    }

    @Override
    public boolean test(String uri) {
        try {
            String path = new URI(uri).getPath();
            if (path != null) {
                String name = new File(path).getName();
                for (int i = 0; i < names.length; ++i) {
                    if (name.equals(names[i])) {
                        if (blacklist) {
                            LOGGER.trace("URI filtered by the blacklisted file name ({}): {}", names[i], uri);
                        }

                        return !blacklist;
                    }
                }
            }
        } catch (URISyntaxException e) {
        }

        if (!blacklist) {
            LOGGER.trace("URI filtered by not being on the file name whitelist: {}", uri);
        }
        return blacklist;
    }
}
