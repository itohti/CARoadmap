package com.caroadmap.api;

import com.caroadmap.data.Boss;
import com.caroadmap.data.PlayerDataDiffUtil;
import com.caroadmap.data.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class PlayerDataBatcher {
    private final String username;
    private final Map<String, ArrayList<Object>> batch;
    private final CARoadmapServer server;
    private final File playerCache;

    public PlayerDataBatcher(String username, CARoadmapServer server) {
        File pluginDir = new File(RuneLite.RUNELITE_DIR, "caroadmap");
        this.username = username;
        this.playerCache = new File(pluginDir, String.format("player_cache_%s.json", username.replace(" ", "_")));
        this.batch = new HashMap<>();
        this.server = server;
        this.batch.put("boss_info", new ArrayList<>());
        this.batch.put("combat_stats", new ArrayList<>());
        this.batch.put("tasks", new ArrayList<>());
    }

    /**
     * Add boss info to backend
     * @param boss is the boss object that will be added to firestore
     * @return true if the operation succeeds.
     */
    public boolean addBossToBatch(Boss boss) {
        try {
            batch.get("boss_info").add(boss.formatBoss());
            return true;
        }
        catch (Exception e) {
            log.error("Could not add boss info in firestore: ", e);
            return false;
        }
    }

    /**
     * Adds a skill to the backend database.
     * @param skillName is the skill name.
     * @param level is the level that is associated with the skill.
     * @return true if it was successful.
     */
    public boolean addSkillToBatch(String skillName, int level) {
        Map<String, Object> skillMap = new HashMap<>();
        skillMap.put(skillName, level);
        skillMap.put("skill_name", skillName);
        try {
            batch.get("combat_stats").add(skillMap);
            return true;
        }
        catch (Exception e) {
            log.error("Could not upload boss to backend: ", e);
            return false;
        }
    }

    /**
     * Adds a task into the backend database.
     * @param task the task that needs to be added.
     * @return true if it was successful.
     */
    public boolean addTaskToBatch(Task task) {
        Map<String, Object> userTaskObj = new HashMap<>();
        userTaskObj.put("Done", task.isDone());
        userTaskObj.put("task_name", task.getTaskName());
        try {
            batch.get("tasks").add(userTaskObj);
            return true;
        }
        catch (Exception e) {
            log.error("Could not upload task to backend: ", e);
            return false;
        }
    }

    public Map<String, ArrayList<Object>> loadPlayerCache() {
        if (!playerCache.exists()) return null;

        try (FileReader reader = new FileReader(playerCache)) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(reader, new TypeReference<Map<String, ArrayList<Object>>>() {});
        }
        catch (IOException e) {
            log.error("Could not read player cache: ", e);
            return null;
        }
    }

    public boolean sendData() {
        Map<String, ArrayList<Object>> localCache = loadPlayerCache();
        boolean updatedFlag = false;
        ObjectMapper mapper = new ObjectMapper();
        Map<String, ArrayList<Object>> batchCopy = mapper.convertValue(batch, new TypeReference<Map<String, ArrayList<Object>>>() {});
        if (localCache == null) {
            log.warn("No player cache found, uploading everything.");
            return server.storePlayerData(username, batch);
        }

        for (Map.Entry<String, ArrayList<Object>> entry : batch.entrySet()) {
            String key = entry.getKey();
            ArrayList<Object> newVal = entry.getValue();
            ArrayList<Object> cachedVal = localCache.getOrDefault(key, new ArrayList<>());

            List<Object> filtered = PlayerDataDiffUtil.filterByKey(key, newVal, cachedVal);
            if (!filtered.isEmpty()) {
                updatedFlag = true;
            }
            batch.put(key, new ArrayList<>(filtered));
        }

        if (!updatedFlag) {
            log.info("No player data changed.");
            return true;
        }

        if (server.storePlayerData(username, batch)){
            if (updatedFlag) {
                try (FileWriter writer = new FileWriter(playerCache)) {
                    String toWrite = mapper.writeValueAsString(batchCopy);
                    writer.write(toWrite);
                    return true;
                }
                catch (IOException e) {
                    log.error("Could not write into cache file: ", e);
                    return false;
                }
            }
        }

        return false;
    }
}
