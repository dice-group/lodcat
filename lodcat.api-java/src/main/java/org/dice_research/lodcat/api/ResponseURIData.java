package org.dice_research.lodcat.api;

import java.util.*;

public class ResponseURIData {
    private Collection<String> descriptions;
    private Collection<String> labels;

    public Collection<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Collection<String> descriptions) {
        this.descriptions = descriptions;
    }

    public Collection<String> getLabels() {
        return labels;
    }

    public void setLabels(Collection<String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "{labels=" + labels + ", descriptions=" + descriptions + "}";
    }
}
