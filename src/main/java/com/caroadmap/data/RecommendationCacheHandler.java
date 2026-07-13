package com.caroadmap.data;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.*;

@Slf4j
public class RecommendationCacheHandler {

    private final File cacheDirectory;
    private final Gson gson;


    public RecommendationCacheHandler() {
        cacheDirectory = new File(
                RuneLite.RUNELITE_DIR,
                "caroadmap/cache"
        );

        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }

        gson = new Gson();
    }


    public void saveCache(
            long characterId,
            RecommendationCache response
    ) {

        File file = getCacheFile(characterId);

        try (Writer writer = new FileWriter(file)) {

            gson.toJson(response, writer);

        } catch (IOException e) {
            log.error("Could not save recommendation cache", e);
        }
    }


    public RecommendationCache loadCache(
            long characterId
    ) {

        File file = getCacheFile(characterId);

        if (!file.exists()) {
            return null;
        }


        try (Reader reader = new FileReader(file)) {

            return gson.fromJson(
                    reader,
                    RecommendationCache.class
            );

        } catch (IOException e) {

            log.error(
                    "Could not load recommendation cache",
                    e
            );

            return null;
        }
    }


    private File getCacheFile(long characterId) {

        return new File(
                cacheDirectory,
                "recommendations_" + characterId + ".json"
        );
    }
}