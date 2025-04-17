package com.caroadmap.api;

import com.caroadmap.dto.GetRecommendationsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

@Singleton
@Slf4j
public class CARoadmapServer {
    private final HttpClient client;

    @Inject
    public CARoadmapServer() {
        log.info("Initialized CARoadmapServer");
        this.client = HttpClient.newHttpClient();
    }

    public boolean storePlayerData(String username, Map<String, ArrayList<Object>> data) {
        try {
            String jsonBody = convertMapToString(data);
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://osrs.izdartohti.org/playerdata?username=%s", encodedUsername)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        catch (InterruptedException | IOException | RuntimeException e) {
            log.error("Could not connect to backend... Did not upload player data: ", e);
            return false;
        }
    }

    public GetRecommendationsResponse getRecommendations(String username, int pointThreshold) throws IOException, InterruptedException {
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://osrs.izdartohti.org/get_recommendations?username=%s&point_threshold=%d", encodedUsername, pointThreshold)))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
            return mapper.readValue(response.body(), GetRecommendationsResponse.class);
        }
        catch (InterruptedException | IOException e) {
            log.error("Error fetching recommendations from server: ", e);
            throw e;
        }
    }

    private String convertMapToString(Map<String, ArrayList<Object>> map) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert map to JSON string.", e);
        }
    }
}


