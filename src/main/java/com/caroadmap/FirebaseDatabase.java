package com.caroadmap;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Handles all logic regarding firestore.
 */
public class FirebaseDatabase {
    private Firestore db;
    private WriteBatch currentBatch;
    private CollectionReference userTasks;
    private CollectionReference userBossInfo;
    private CollectionReference userStats;
    private int batchCount;
    private static final int BATCH_LIMIT = 500;
    /**
     * Constructor that sets up Admin SDK access to firebase project.
     */
    public FirebaseDatabase(String username) {
        try {
            InputStream serviceAccount =
                    getClass().getResourceAsStream("/caroadmap-firebase-adminsdk-fbsvc-ace27393f8.json");
            if (serviceAccount == null) {
                throw new IOException("Could not find firebase credentials");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            try {
                this.db = FirestoreClient.getFirestore();
                userTasks = db.collection("users").document(username).collection("tasks");
                userBossInfo = db.collection("users").document(username).collection("boss_info");
                userStats = db.collection("users").document(username).collection("combat_stats");
                Map<String, String> displayName = new HashMap<>();
                displayName.put("display_name", username);
                db.collection("users").document(username).set(displayName);
                this.currentBatch = db.batch();
            }
            catch (Exception e) {
                System.err.println("Something went wrong: " + e.getMessage());
            }

            batchCount = 0;
        }
        catch (IOException e) {
            // find out what to do here.
            System.err.println("Could not find Firebase credentials...");
        }
        catch (Exception e) {
            System.err.println("Something went wrong: " + e.getMessage());
        }
    }

    /**
     * Add boss info to firestore
     * @param boss is the boss object that will be added to firestore
     * @return true if the operation succeeds.
     */
    public boolean addBossToBatch(Boss boss) {
        try {
            DocumentReference docRef = userBossInfo.document(boss.getBossName());
            currentBatch.set(docRef, boss.formatBoss());
            batchCount++;

            if (batchCount >= BATCH_LIMIT) {
                commitBatch();
            }

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
        Map<String, Integer> skillMap = new HashMap<>();
        skillMap.put(skillName, level);
        try {
            DocumentReference docRef = userStats.document(skillName);
            currentBatch.set(docRef, skillMap);
            batchCount++;

            if (batchCount >= BATCH_LIMIT) {
                commitBatch();
            }

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
        // first format task into a Map<String, Object> object.
        try {
            DocumentReference docRef = userTasks.document(task.getTaskName());
            Map<String, Object> userTaskObj = new HashMap<>();
            userTaskObj.put("Done", task.isDone());
            currentBatch.set(docRef, userTaskObj);
            batchCount++;

            if (batchCount >= BATCH_LIMIT) {
                commitBatch();
            }

            return true;
        }
        catch (Exception e) {
            System.err.println("Could not upload task into Firestore: " + e.getMessage());
            return false;
        }
    }

    /**
     * Commits the batch to firestore.
     * @return true if it successfully wrote the batch to firestore.
     */
    public boolean commitBatch() {
        if (batchCount == 0) {
            return true; // nothing to commit
        }

        try {
            ApiFuture<List<WriteResult>> future = currentBatch.commit();
            List<WriteResult> result = future.get();

            System.out.println("Commited " + batchCount + " writes.");
            currentBatch = db.batch();
            batchCount = 0;

            return true;
        }
        catch (InterruptedException | ExecutionException e) {
            System.err.println("Could not get future of batch.commit()...");
            return false;
        }
    }

    public void cleanUp() {
        for (FirebaseApp app: FirebaseApp.getApps()) {
            app.delete();
        }
    }
}
