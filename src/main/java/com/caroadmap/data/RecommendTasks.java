package com.caroadmap.data;

import com.caroadmap.api.CARoadmapServer;
import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.RecommendedTaskDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

@Slf4j
public class RecommendTasks {
    @Getter
    private ArrayList<Task> recommendedTasks;
    private final RecommendationCacheHandler cacheHandler;

    @Setter
    private SortingType sortingType = SortingType.SCORE;
    @Setter
    private Boolean ascending = true;

    private final CARoadmapServer server;

    @Inject
    public RecommendTasks(CARoadmapServer server, ConfigManager configManager, RecommendationCacheHandler cacheHandler) {
        this.sortingType = configManager.getConfiguration("CARoadmap", "sortingType", SortingType.class);
        if (sortingType == null) {
            sortingType = SortingType.SCORE;
        }

        this.ascending = configManager.getConfiguration("CARoadmap", "isAscending", Boolean.class);
        if (ascending == null) {
            ascending = true;
        }

        this.recommendedTasks = new ArrayList<>();
        this.cacheHandler = cacheHandler;
        this.server = server;
    }

    public boolean isAscending() {
        return ascending;
    }

    /**
     * Gets the recommendations from the server OR locally. First it will be locally then from the server.
     * @param characterId the character id of the player
     */
    public void getRecommendations(long characterId) {
        log.info("get Recommendations was called");
        if (hasValidCache(characterId)) {
            log.info("Using cached recommendations");
            recommendedTasks = loadCachedRecommendations(characterId);
            return;
        }


        log.info("Fetching fresh recommendations");
        fetchAndCacheRecommendationsFromServer(characterId);
    }

    private ArrayList<Task> loadCachedRecommendations(long characterId) {

        RecommendationCache cache =
                cacheHandler.loadCache(characterId);

        if (cache == null) {
            return new ArrayList<>();
        }

        return convertRecommendations(cache);
    }

    private ArrayList<Task> convertRecommendations(
            RecommendationCache response
    ) {

        ArrayList<Task> tasks = new ArrayList<>();

        for (RecommendedTaskDTO task : response.getRecommendedTasks()) {

            try {

                Task newTask = new Task(
                        task.getBoss_name(),
                        task.getTitle(),
                        task.getDescription(),
                        TaskType.valueOf(
                                task.getType()
                                        .toUpperCase()
                                        .replace(" ", "_")
                        ),
                        task.getPoints(),
                        false
                );


                newTask.setScore(
                        task.getScore()
                );

                newTask.setCompletionProbability(
                        task.getCompletion_probability()
                );

                newTask.setCompletionPercent(
                        task.getCompletion_percent()
                );

                newTask.setKillsRemaining(
                        task.getKills_remaining()
                );

                newTask.setCurrentKills(
                        task.getCurrent_kills()
                );

                newTask.setRequiredKills(
                        task.getRequired_kills()
                );

                tasks.add(newTask);


            } catch(Exception e) {
                log.error(
                        "Failed converting recommendation",
                        e
                );
            }
        }


        sortRecommendations(tasks);

        return tasks;
    }

    private void fetchAndCacheRecommendationsFromServer(long characterId) {

        try {

            GetRecommendationsResponse response =
                    server.getRecommendations(characterId);


            if (response.getError() != null) {
                log.error(
                        "Server error: {}",
                        response.getError()
                );
                return;
            }


            RecommendationCache cache =
                    new RecommendationCache();

            cache.setCharacterId(characterId);
            cache.setGeneratedAt(
                    response.getGeneratedAt()
            );
            cache.setRecommendedTasks(
                    response.getRecommendedTasks()
            );


            cacheHandler.saveCache(characterId, cache);


            this.recommendedTasks =
                    convertRecommendations(cache);


        } catch(IOException | InterruptedException e) {

            log.error(
                    "Could not fetch recommendations",
                    e
            );
        }
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

    private boolean hasValidCache(long characterId) {

        RecommendationCache cache =
                cacheHandler.loadCache(characterId);


        if (cache == null) {
            return false;
        }

        Instant generatedAt =
                Instant.parse(cache.getGeneratedAt());


        return generatedAt
                .plus(7, ChronoUnit.DAYS)
                .isAfter(Instant.now());
    }

}
