package com.caroadmap.api;

import com.caroadmap.CARoadmapConfig;
import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.RegisterDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class CARoadmapServer {
    private final HttpClient client;
    private final Gson gson;

    @Setter
    @Getter
    private String apiKey;

    @Inject
    private ConfigManager configManager;

    private static final File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");

    @Inject
    public CARoadmapServer(Gson gson) {
        log.info("Initialized CARoadmapServer");
        this.client = HttpClient.newHttpClient();
        this.gson = gson;
    }

    public void register(String username) {
        Map<String, String> data = new HashMap<>();
        data.put("display_name", username);
        try {
            String json = gson.toJson(data);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://osrs.izdartohti.org/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            RegisterDTO registerResponse = gson.fromJson(response.body(), RegisterDTO.class);
            this.apiKey = registerResponse.getApi_key();
        } catch (IOException | InterruptedException e) {
            log.error("Could not register user with error: ", e);
        }
    }

    public void fetchAndCachePlayerData(String username) {
        File cacheFile = new File(pluginDir, String.format("player_cache_%s.json", username.replace(" ", "_")));
        try (FileWriter writer = new FileWriter(cacheFile)) {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("http://osrs.izdartohti.org/playerdata?username=%s", encodedUsername)))
                    .header("X-API-Key", this.apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                writer.write(response.body());
            } else {
                log.warn("Failed with status code: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Could not fetch player data: ", e);
        }
    }

    public boolean storePlayerData(String username, Map<String, ArrayList<Object>> data) {
        log.info("storing player data");
        try {
            String jsonBody = gson.toJson(data);
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://osrs.izdartohti.org/playerdata?username=%s", encodedUsername)))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", this.apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                log.warn("API key rejected. Attempting re-registration...");
                register(username);
                configManager.setConfiguration("CARoadmap", "apiKey", this.apiKey);

                request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format("https://osrs.izdartohti.org/playerdata?username=%s", encodedUsername)))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", this.apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            return response.statusCode() == 200;
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.error("Could not connect to backend... Did not upload player data: ", e);
            return false;
        }
    }

    public boolean updatePlayerTask(String username, String taskName) {
        try {
            Map<String, String> jsonBody = new HashMap<>();
            jsonBody.put("task_name", taskName);
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String json = gson.toJson(jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://osrs.izdartohti.org/mark_completed?username=%s", encodedUsername)))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", this.apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            log.error("Could not update player task", e);
            return false;
        }
    }

    public GetRecommendationsResponse getRecommendations(String username, int pointThreshold)
            throws IOException, InterruptedException {
        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(
                            "https://osrs.izdartohti.org/get_recommendations?username=%s&point_threshold=%d",
                            encodedUsername, pointThreshold)))
                    .header("X-API-Key", this.apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                register(username);
                request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(
                                "https://osrs.izdartohti.org/get_recommendations?username=%s&point_threshold=%d",
                                encodedUsername, pointThreshold)))
                        .header("X-API-Key", this.apiKey)
                        .GET()
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }

            return gson.fromJson(response.body(), GetRecommendationsResponse.class);
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching recommendations from server: ", e);
            throw e;
        }
    }
}
