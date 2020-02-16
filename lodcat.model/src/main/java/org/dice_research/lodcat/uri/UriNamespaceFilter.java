package org.dice_research.lodcat.uri;

/**
 * A filter based on a list of given URI namespaces and a flag indicating
 * whether it is a blacklist of whitelist filter.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class UriNamespaceFilter implements UriFilter {

    private boolean blacklist;
    private String[] namespaces;

    public UriNamespaceFilter(String[] namespaces) {
        this(namespaces, true);
    }

    public UriNamespaceFilter(String[] namespaces, boolean blacklist) {
        this.blacklist = blacklist;
        this.namespaces = namespaces;
    }

    @Override
    public boolean test(String uri) {
        if (blacklist) {
            // Use namespaces as blacklist
            for (int i = 0; i < namespaces.length; ++i) {
                if (uri.startsWith(namespaces[i])) {
                    return false;
                }
            }
        } else {
            // Use namespaces as whitelist
            for (int i = 0; i < namespaces.length; ++i) {
                if (uri.startsWith(namespaces[i])) {
                    return true;
                }
            }
        }
        return !blacklist;
    }

}
