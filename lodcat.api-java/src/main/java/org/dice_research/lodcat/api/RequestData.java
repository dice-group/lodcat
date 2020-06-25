package org.dice_research.lodcat.api;

import java.util.*;

public class RequestData {
    private Collection<String> uris;

    public RequestData(Collection<String> uris) {
        this.uris = uris;
    }

    public Collection<String> getUris() {
        return uris;
    }
}
