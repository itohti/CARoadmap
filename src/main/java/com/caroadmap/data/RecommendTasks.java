package com.caroadmap.data;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.RecommendedTaskDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Slf4j
public class RecommendTasks {
    @Getter
    private ArrayList<Task> recommendedTasks;
    private final CSVHandler csvHandler;


    private final CARoadmapServer server;

    @Inject
    public RecommendTasks(CARoadmapServer server, CSVHandler csvHandler) {
        log.info("Initialized RecommendTasks");
        this.recommendedTasks = new ArrayList<>();
        this.csvHandler = csvHandler;
        this.server = server;
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
        try {
            GetRecommendationsResponse recommendationsList = server.getRecommendations(username, pointThreshold);

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
                    csvHandler.createTask(newTask);
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
