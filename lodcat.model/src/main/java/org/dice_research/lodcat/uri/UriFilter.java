package org.dice_research.lodcat.uri;

import java.util.function.Predicate;

import com.carrotsearch.hppc.predicates.ObjectPredicate;

/**
 * A filter for URIs.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface UriFilter extends ObjectPredicate<String>, Predicate<String> {

    @Override
    public default boolean apply(String uri) {
        return test(uri);
    }
}
