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

public class Client {
    private static TypeReference<Map<String, ResponseURIData>> responseDataType = new TypeReference<>(){};

    private static String DEFAULT_BASE = "http://lodcat-labels.cs.upb.de/";
    private static String DETAILS_PATH = "uri/details";

    private String baseURL;

    private ObjectMapper mapper = new ObjectMapper();

    public Client(String baseURL) {
        this.baseURL = baseURL;
    }

    public Client() {
        this(DEFAULT_BASE);
    }

    public Map<String, ResponseURIData> getDetails(Collection<String> uris) throws IOException {
        RequestData data = new RequestData(uris);
        HttpPost post = new HttpPost(new URL(new URL(baseURL), DETAILS_PATH).toString());
        post.setEntity(new StringEntity(mapper.writeValueAsString(data), ContentType.APPLICATION_JSON));
        post.setHeader("Accept", ContentType.APPLICATION_JSON.toString());

        try (
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(post)
        ) {
            return mapper.readValue(EntityUtils.toString(response.getEntity()), responseDataType);
        }
    }
}
