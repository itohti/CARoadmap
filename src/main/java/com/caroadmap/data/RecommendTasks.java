package com.caroadmap.data;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.RecommendedTaskDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Slf4j
public class RecommendTasks {
    @Getter
    private ArrayList<Task> recommendedTasks;
    private final CSVHandler csvHandler;

    @Setter
    private SortingType sortingType = SortingType.SCORE;
    @Setter
    private Boolean ascending = true;

    private final CARoadmapServer server;

    @Inject
    public RecommendTasks(CARoadmapServer server, CSVHandler csvHandler, ConfigManager configManager) {
        this.sortingType = configManager.getConfiguration("CARoadmap", "sortingType", SortingType.class);
        if (sortingType == null) {
            sortingType = SortingType.SCORE;
        }

        this.ascending = configManager.getConfiguration("CARoadmap", "isAscending", Boolean.class);
        if (ascending == null) {
            ascending = true;
        }

        this.recommendedTasks = new ArrayList<>();
        this.csvHandler = csvHandler;
        this.server = server;
    }

    public boolean isAscending() {
        return ascending;
    }

    /**
     * Gets the recommendations from the server OR locally. First it will be locally then from the server.
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

        try (BufferedReader reader = new BufferedReader(new FileReader(csvHandler.getCsvPath(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                log.warn("CSV file is empty.");
                return taskList;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line); // <-- Updated

                if (values.length < CSVColumns.values().length) {
                    continue;
                }

                try {
                    Task task = new Task(
                            values[CSVColumns.BOSS_NAME.ordinal()],
                            values[CSVColumns.TASK_NAME.ordinal()],
                            values[CSVColumns.TASK_DESCRIPTION.ordinal()],
                            values[CSVColumns.TYPE.ordinal()],
                            values[CSVColumns.TIER.ordinal()],
                            values[CSVColumns.DONE.ordinal()]
                    );

                    if (CSVColumns.SCORE.ordinal() < values.length) {
                        double score = Double.parseDouble(values[CSVColumns.SCORE.ordinal()]);
                        task.setScore(score);
                    }

                    taskList.add(task);
                } catch (Exception e) {
                    log.warn("Could not parse task from CSV line: {}", line, e);
                }
            }

            sortRecommendations(taskList);
        } catch (Exception e) {
            log.warn("Could not load local recommendation list. Will try server next.", e);
        }

        return taskList;
    }

    private void fetchAndCacheRecommendationsFromServer(String username, int pointThreshold) {
        ArrayList<Task> recommendedTasks = new ArrayList<>();
        try {
            GetRecommendationsResponse recommendationsList = server.getRecommendations(username, pointThreshold);
            if (recommendationsList.getError() != null) {
                log.error("There was an error on the server side: {}", recommendationsList.getError());
            }

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
                    newTask.setScore(task.getScore());
                    recommendedTasks.add(newTask);
                    csvHandler.createTask(newTask);
                } catch (Exception e) {
                    log.error("Failed to parse task from server response", e);
                }
            }

            sortRecommendations(recommendedTasks);
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching recommendations from server: ", e);
        }

        this.recommendedTasks = recommendedTasks;
    }

    private void sortRecommendations(ArrayList<Task> tasks) {
        if (sortingType == SortingType.SCORE) {
            tasks.sort(this.ascending ? Task.byScore() : Task.byScore().reversed());
        } else if (sortingType == SortingType.BOSS) {
            tasks.sort(this.ascending ? Task.byBoss() : Task.byBoss().reversed());
        } else if (sortingType == SortingType.TIER) {
            tasks.sort(this.ascending ? Task.byTier() : Task.byTier().reversed());
        }
    }

    private String[] parseCsvLine(String line) {
        ArrayList<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        // Double quote -> add literal quote
                        current.append('"');
                        i++;
                    } else {
                        // End of quoted field
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
