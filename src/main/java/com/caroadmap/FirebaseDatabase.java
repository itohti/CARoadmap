package com.caroadmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles all logic regarding firestore.
 */
public class FirebaseDatabase {
    private HttpClient client;
    private String username;
    private Map<String, ArrayList<Object>> batch;
    /**
     * Constructor that sets up Admin SDK access to firebase project.
     */
    public FirebaseDatabase(String username) {
        this.username = username;
        this.client = HttpClient.newHttpClient();
        this.batch = new HashMap<>();
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
        String jsonBody = convertMapToString(batch);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://osrs.izdartohti.org:8080/playerdata?username=%s", username)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        }
        catch (InterruptedException | IOException e) {
            System.err.println("Could not connect to backend... Did not upload player data: " + e);
            return false;
        }
    }

    private String convertMapToString(Map<String, ArrayList<Object>> map) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            System.err.println("Could not convert map to string.");
            return "";
        }
    }
}
