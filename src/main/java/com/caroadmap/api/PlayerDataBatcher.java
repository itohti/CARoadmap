package com.caroadmap.api;

import com.caroadmap.data.Boss;
import com.caroadmap.data.PlayerDataDiffUtil;
import com.caroadmap.data.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class PlayerDataBatcher {
    private static final String BOSS_INFO_KEY = "boss_info";
    private static final String COMBAT_STATS_KEY = "combat_stats";
    private static final String TASKS_KEY = "tasks";

    private final String username;
    private final Map<String, ArrayList<Object>> batch;
    private final CARoadmapServer server;
    private final File playerCache;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public PlayerDataBatcher(String username, CARoadmapServer server) {
        this.username = username;
        this.server = server;

        File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");
        this.playerCache = new File(pluginDir, String.format("player_cache_%s.json", username.replace(" ", "_")));

        this.batch = new HashMap<>();
        batch.put(BOSS_INFO_KEY, new ArrayList<>());
        batch.put(COMBAT_STATS_KEY, new ArrayList<>());
        batch.put(TASKS_KEY, new ArrayList<>());

    }

    public boolean addBossToBatch(Boss boss) {
        try {
            batch.get(BOSS_INFO_KEY).add(boss.formatBoss());
            return true;
        } catch (Exception e) {
            log.error("Could not add boss info to batch: ", e);
            return false;
        }
    }

    public boolean addSkillToBatch(String skillName, int level) {
        Map<String, Object> skillMap = new HashMap<>();
        skillMap.put("skill_name", skillName);
        skillMap.put("level", level);

        try {
            batch.get(COMBAT_STATS_KEY).add(skillMap);
            return true;
        } catch (Exception e) {
            log.error("Could not add skill info to batch: ", e);
            return false;
        }
    }

    public boolean addTaskToBatch(Task task) {
        Map<String, Object> userTaskObj = new HashMap<>();
        userTaskObj.put("Done", task.isDone());
        userTaskObj.put("task_name", task.getTaskName());

        try {
            batch.get(TASKS_KEY).add(userTaskObj);
            return true;
        } catch (Exception e) {
            log.error("Could not add task info to batch: ", e);
            return false;
        }
    }

    public Map<String, ArrayList<Object>> loadPlayerCache() {
        if (!playerCache.exists()) {
            return null;
        }
        try (Reader reader = new FileReader(playerCache)) {
            Type type = new TypeToken<Map<String, ArrayList<Object>>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            log.error("Could not read player cache from file: ", e);
            return null;
        }
    }

    public boolean sendData() {
        Map<String, ArrayList<Object>> localCache = loadPlayerCache();
        log.info("testing");
        boolean hasUpdates = false;

        // Deep copy batch by serializing and deserializing with Gson
        String batchJson = gson.toJson(batch);
        Type type = new TypeToken<Map<String, ArrayList<Object>>>() {}.getType();
        Map<String, ArrayList<Object>> batchCopy = gson.fromJson(batchJson, type);

        if (localCache == null) {
            log.warn("No player cache found, uploading full batch.");
            return server.storePlayerData(username, batch);
        }


        // Filter out unchanged data by comparing to cache
        for (Map.Entry<String, ArrayList<Object>> entry : batch.entrySet()) {
            String key = entry.getKey();
            ArrayList<Object> newData = entry.getValue();
            ArrayList<Object> cachedData = localCache.getOrDefault(key, new ArrayList<>());

            List<Object> filteredData = PlayerDataDiffUtil.filterByKey(key, newData, cachedData);
            if (!filteredData.isEmpty()) {
                hasUpdates = true;
            }

            // Replace batch data with filtered data for sending
            batch.put(key, new ArrayList<>(filteredData));
        }

        log.info("sending data");

        if (!hasUpdates) {
            log.info("No player data changes detected; skipping upload.");
            return true;
        }

        if (server.storePlayerData(username, batch)) {
            try (Writer writer = new FileWriter(playerCache)) {
                gson.toJson(batchCopy, writer);
                return true;
            } catch (IOException e) {
                log.error("Failed to write updated player cache to file: ", e);
                return false;
            }
        }
        else {
            log.error("Something went wrong.");
        }

        return false;
    }
}
