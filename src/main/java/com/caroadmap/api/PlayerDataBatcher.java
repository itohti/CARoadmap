package com.caroadmap.api;

import com.caroadmap.data.Boss;
import com.caroadmap.data.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerDataBatcher {
    private final String username;
    private Map<String, ArrayList<Object>> batch;
    private final CARoadmapServer server;

    public PlayerDataBatcher(String username, CARoadmapServer server) {
        this.username = username;
        this.batch = new HashMap<>();
        this.server = server;
        this.batch.put("boss_info", new ArrayList<>());
        this.batch.put("combat_stats", new ArrayList<>());
        this.batch.put("tasks", new ArrayList<>());
    }

    /**
     * Add boss info to firestore
     * @param boss is the boss object that will be added to firestore
     * @return true if the operation succeeds.
     */
    public boolean addBossToBatch(Boss boss) {
        try {
            batch.get("boss_info").add(boss.formatBoss());
            return true;
        }
        catch (Exception e) {
            System.err.println("Could not add boss info in firestore: " + e);
            return false;
        }
    }

    /**
     * Adds a skill to the Firestore database.
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
            System.err.println("Could not upload boss into Firestore: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a task into the Firestore database.
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
            System.err.println("Could not upload task into Firestore: " + e.getMessage());
            return false;
        }
    }

    public boolean sendData() {
        return server.storePlayerData(username, batch);
    }
}
