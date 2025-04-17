package com.caroadmap.data;

import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.RecommendedTaskDTO;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Slf4j
public class RecommendTasks {
    @Getter
    private ArrayList<Task> recommendedTasks;
    private final CSVHandler csvHandler = new CSVHandler();

    public RecommendTasks() {
        recommendedTasks = new ArrayList<>();
    }

    /**
     * Gets the recommendations from the server OR locally. First it will be locally than it would be from the server.
     * @param username the username of the player
     * @param pointThreshold the point goal the user is trying to hit.
     */
    public void getRecommendations(String username, int pointThreshold) {
        ArrayList<Task> localTasks = tryLoadLocalRecommendations();
        if (!localTasks.isEmpty()) {
            log.info("Loaded recommendations from local CSV.");
            this.recommendedTasks = localTasks;
            return;
        }

        log.info("Local CSV empty or missing. Fetching from server...");
        fetchAndCacheRecommendationsFromServer(username, pointThreshold);
    }

    private ArrayList<Task> tryLoadLocalRecommendations() {
        ArrayList<Task> taskList = new ArrayList<>();

        try (Reader reader = new FileReader(csvHandler.getCsvPath(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader(CSVColumns.class)
                    .setSkipHeaderRecord(true)
                    .get()
                    .parse(reader);

            for (CSVRecord record : records) {
                Task task = new Task(
                        record.get(CSVColumns.BOSS_NAME),
                        record.get(CSVColumns.TASK_NAME),
                        record.get(CSVColumns.TASK_DESCRIPTION),
                        record.get(CSVColumns.TYPE),
                        record.get(CSVColumns.TIER),
                        record.get(CSVColumns.DONE)
                );
                taskList.add(task);
            }
        } catch (IOException e) {
            log.warn("Could not load local recommendation list. Will try server next.", e);
        }

        return taskList;
    }

    private void fetchAndCacheRecommendationsFromServer(String username, int pointThreshold) {
        ArrayList<Task> recommendedTasks = new ArrayList<>();

        HttpClient client = HttpClient.newHttpClient();
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://osrs.izdartohti.org/get_recommendations?username=%s&point_threshold=%d", encodedUsername, pointThreshold)))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
            GetRecommendationsResponse recommendationsList = mapper.readValue(response.body(), GetRecommendationsResponse.class);

            for (RecommendedTaskDTO task : recommendationsList.recommended_tasks) {
                try {
                    Task newTask = new Task(
                            task.getMonster(),
                            task.getTask_name(),
                            task.getDescription(),
                            TaskType.valueOf(task.getType().toUpperCase().replace(" ", "_")),
                            task.getTier(),
                            false
                    );
                    recommendedTasks.add(newTask);
                    csvHandler.createTask(newTask); // 💾 Save to local CSV
                } catch (Exception e) {
                    log.error("Failed to parse task from server response", e);
                }
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error fetching recommendations from server: ", e);
        }

        this.recommendedTasks = recommendedTasks;
    }
}
