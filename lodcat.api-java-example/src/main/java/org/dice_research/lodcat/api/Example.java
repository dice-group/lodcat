package org.dice_research.lodcat.api;

import java.util.*;
import org.dice_research.lodcat.api.Client;

public class Example {
    public static void main(String[] args) throws Exception {
        try (Client client = new Client()) {
            ArrayList<String> uris = new ArrayList<>();
            uris.add("http://ontologi.es/days#TuesdayInterval");
            System.err.println(client.getDetails(uris));
            uris.add("http://ontologi.es/rail/void#dataset_0.1");
            System.err.println(client.getLabels(uris));
        }
    }
}
