package org.dice_research.lodcat.uri;

import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter based on a list of given file extensions and a flag indicating
 * whether it is a blacklist of whitelist filter.
 */
public class UriFileExtensionFilter implements UriFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriFileExtensionFilter.class);

    private boolean blacklist;
    private String[] extensions;

    public UriFileExtensionFilter(String[] extensions) {
        this(extensions, true);
    }

    public UriFileExtensionFilter(String[] extensions, boolean blacklist) {
        this.blacklist = blacklist;
        this.extensions = extensions;
    }

    @Override
    public boolean test(String uri) {
        String path;
        try {
            path = new URI(uri).getPath();
        } catch (URISyntaxException e) {
            path = uri;
        }

        if (path != null) {
            for (int i = 0; i < extensions.length; ++i) {
                if (path.endsWith("." + extensions[i])) {
                    if (blacklist) {
                        LOGGER.trace("URI filtered by the blacklisted extension ({}): {}", extensions[i], uri);
                    }

                    return !blacklist;
                }
            }
        }

        if (!blacklist) {
            LOGGER.trace("URI filtered by not being on the extension whitelist: {}", uri);
        }
        return blacklist;
    }
}
