/**
 * tapioca.core - ${project.description}
 * Copyright © 2015 Data Science Group (DICE) (michael.roeder@uni-paderborn.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dice_research.lodcat.data;

import org.dice_research.topicmodeling.utils.doc.AbstractSimpleDocumentProperty;
import org.dice_research.topicmodeling.utils.doc.ParseableDocumentProperty;
import org.dice_research.topicmodeling.utils.doc.StringContainingDocumentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;

/**
 * Property storing a map of (URI, count) pairs. 
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class UriCounts extends AbstractSimpleDocumentProperty<ObjectLongOpenHashMap<String>> implements
        StringContainingDocumentProperty, ParseableDocumentProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(UriCounts.class);

    private static final char URI_COUNT_DELIMITTER = '>';
    private static final char PAIR_DELIMITTER = '|';

    private static final long serialVersionUID = 1L;

    public UriCounts() {
        super(null);
    }

    public UriCounts(ObjectLongOpenHashMap<String> countedURIs) {
        super(countedURIs);
    }

    public void parseValue(String value) {
        ObjectLongOpenHashMap<String> countedURIs = new ObjectLongOpenHashMap<String>();
        parse(value, countedURIs);
        set(countedURIs);
    }

    public String getStringValue() {
        @SuppressWarnings("unchecked")
        ObjectLongOpenHashMap<String> countedURIs = (ObjectLongOpenHashMap<String>) getValue();
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < countedURIs.allocated.length; ++i) {
            if (countedURIs.allocated[i]) {
                if (first) {
                    first = false;
                } else {
                    result.append(PAIR_DELIMITTER);
                }
                result.append(((Object[]) countedURIs.keys)[i]);
                result.append(URI_COUNT_DELIMITTER);
                result.append(countedURIs.values[i]);
            }
        }
        return result.toString();
    }

    protected void parse(String value, ObjectLongOpenHashMap<String> countedURIs) {
        // URI>count|URI>count|URI>count...
        char chars[] = value.toCharArray();
        int start = 0;
        // 0 - reading URI
        // 1 - reading count
        int state = 0;
        String uri = null;
        long count;
        for (int i = 0; i < chars.length; ++i) {
            switch (chars[i]) {
            case URI_COUNT_DELIMITTER: {
                // found the end of the URI
                uri = new String(value.substring(start, i));
                state = 1;
                start = i + 1;
                break;
            }
            case PAIR_DELIMITTER: {
                try {
                    count = Long.parseLong(value.substring(start, i));
                    countedURIs.put(uri, count);
                } catch (NumberFormatException e) {
                    LOGGER.error("Couldn't parse long that has been expected to be the count of the URI \"{}\". Ignoring this URI.");
                }
                state = 0;
                start = i + 1;
            }
            default: {
                // nothing to do
            }
            }
        }
        if ((state == 1) && (start < value.length())) {
            count = Long.parseLong(value.substring(start));
            countedURIs.put(uri, count);
        }
    }
}
