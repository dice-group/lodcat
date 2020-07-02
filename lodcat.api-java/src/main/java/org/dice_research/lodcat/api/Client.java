package org.dice_research.lodcat.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.URL;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Client class for the labels API.
 */
public class Client implements Closeable {
    private static TypeReference<Map<String, ResponseURIData>> responseDataType = new TypeReference<>(){};

    private static String DEFAULT_BASE = "http://lodcat-labels.cs.upb.de/";
    private static String DETAILS_PATH = "uri/details";
    private static String LABELS_PATH = "uri/labels";
    private static String DESCRIPTIONS_PATH = "uri/descriptions";

    private String baseURL;
    private CloseableHttpClient httpClient;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for a custom API address.
     */
    public Client(String baseURL) {
        this.baseURL = baseURL;
        httpClient = HttpClients.createDefault();
    }

    /**
     * Constructor for the default API address.
     */
    public Client() {
        this(DEFAULT_BASE);
    }

    private Map<String, ResponseURIData> getDetails(String apiPath, Collection<String> uris) throws IOException {
        RequestData data = new RequestData(uris);
        HttpPost post = new HttpPost(new URL(new URL(baseURL), apiPath).toString());
        post.setEntity(new StringEntity(mapper.writeValueAsString(data), ContentType.APPLICATION_JSON));
        post.setHeader("Accept", ContentType.APPLICATION_JSON.toString());

        try (
            CloseableHttpResponse response = httpClient.execute(post)
        ) {
            return mapper.readValue(EntityUtils.toString(response.getEntity()), responseDataType);
        }
    }

    /**
     * Get all available details for a collection of specified URIs.
     *
     * @param uris a collection of URIs to look up.
     * @return a map from URIs to details objects
     * @throws IOException
     */
    public Map<String, ResponseURIData> getDetails(Collection<String> uris) throws IOException {
        return getDetails(DETAILS_PATH, uris);
    }

    /**
     * Get labels for a collection of specified URIs.
     *
     * @param uris a collection of URIs to look up.
     * @return a map from URIs to details objects
     * @throws IOException
     */
    public Map<String, ResponseURIData> getLabels(Collection<String> uris) throws IOException {
        return getDetails(LABELS_PATH, uris);
    }

    /**
     * Get descriptions for a collection of specified URIs.
     *
     * @param uris a collection of URIs to look up.
     * @return a map from URIs to details objects
     * @throws IOException
     */
    public Map<String, ResponseURIData> getDescriptions(Collection<String> uris) throws IOException {
        return getDetails(DESCRIPTIONS_PATH, uris);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
