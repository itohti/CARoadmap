package com.caroadmap.api;

import com.caroadmap.data.Boss;
import com.caroadmap.data.Task;
import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.TaskDTO;
import com.caroadmap.dto.TaskFromBossResponse;
import com.google.gson.Gson;
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
    private final String SERVER_URL = "https://kxin971pll.execute-api.us-east-1.amazonaws.com";
//    private final String SERVER_URL = "http://localhost:8080";
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

    public boolean storeCharacterData(String username, long accountHash, Map<String, ArrayList<Object>> data) {
        log.info("storing character data");

        Map<String, Object> dataToSend = new HashMap<>();

        dataToSend.put("username", username);
        dataToSend.put("accountHash", accountHash);
        dataToSend.put("character_details", data);
        log.info(dataToSend.toString());

        try {
            String jsonBody = gson.toJson(dataToSend);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/characterdata"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.error("Could not insert character data.");
            return false;
        }
    }

    public boolean updatePlayerBossData(long accountHash, String bossName, int killCount) {
        log.info("Updating kill count for boss {}", bossName);
        Map<String, Object> dataToSend = new HashMap<>();

        dataToSend.put("character_id", accountHash);
        dataToSend.put("boss_name", bossName);
        dataToSend.put("kc", killCount);

        try {
            String jsonBody = gson.toJson(dataToSend);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/store_player_boss_data"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.error("Could not insert player boss data.");
            return false;
        }
    }

    public boolean updatePlayerTaskStatus(long accountHash, String taskTitle) {
        log.info("Updating combat achievement task [{}] to done", taskTitle);
        Map<String, Object> dataToSend = new HashMap<>();

        dataToSend.put("character_id", accountHash);
        dataToSend.put("task_name", taskTitle);
        dataToSend.put("is_done", true);

        try {
            String jsonBody = gson.toJson(dataToSend);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/store_player_task_status"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200;
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.error("Could not insert player task status.");
            return false;
        }
    }

    /**
     * This function will fetch the tasks from the boss.
     * @return an array of tasks from the boss inputted. On error, it will return an EMPTY array.
     */
    public TaskDTO[] fetchTaskFromBoss(String boss, long accountHash) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("character_id", accountHash);
        payload.put("boss_name", boss);

        try {
            String jsonBody = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/get_tasks"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            TaskFromBossResponse parsedResponse = gson.fromJson(response.body(), TaskFromBossResponse.class);

            return parsedResponse.getTasks().toArray(new TaskDTO[0]);
        }
        catch (Exception e) {
            log.error("Could not fetch task information on this boss {} because of {}", boss, e.toString());
            return new TaskDTO[0];
        }
    }

    public GetRecommendationsResponse getRecommendations(long characterId)
            throws IOException, InterruptedException {

        try {
            Map<String, Object> dataToSend = new HashMap<>();

            dataToSend.put("character_id", characterId);

            String jsonBody = gson.toJson(dataToSend);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/get_recommendations"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("Failed to get recommendations. Status: {} Body: {}",
                        response.statusCode(),
                        response.body()
                );
                return null;
            }

            return gson.fromJson(
                    response.body(),
                    GetRecommendationsResponse.class
            );

        } catch (IOException | InterruptedException | RuntimeException e) {
            log.error("Error fetching recommendations from server: ", e);
            throw e;
        }
    }
}
