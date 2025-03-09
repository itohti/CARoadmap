package com.caroadmap;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handles all logic regarding firestore.
 */
public class FirebaseDatabase {
    private Firestore db;
    /**
     * Constructor that sets up Admin SDK access to firebase project.
     */
    public FirebaseDatabase() {
        try {
            FileInputStream serviceAccount =
                    new FileInputStream("./src/main/resources/caroadmap-firebase-adminsdk-fbsvc-ace27393f8.json");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options, "CARoadmap");
            System.out.println("FirebaseApp initialized...");
            this.db = FirestoreClient.getFirestore(); // hanging here.
            System.out.println("Fetched Firestore...");
        }
        catch (IOException e) {
            // find out what to do here.
            System.err.println("Could not find Firebase credentials...");
        }
    }

    /**
     * Adds a task into the Firestore database.
     * @param collection the collection name.
     * @param task the task that needs to be added.
     * @return returns the result of the operation.
     */
    public String addTaskToFirestore(String collection, Task task) {
        // first format task into a Map<String, Object> object.
        Map<String, Object> taskObj = task.formatTask();
        try {
            System.out.println("Attempting to upload task to collection: " + collection);
            ApiFuture<DocumentReference> future = db.collection(collection).add(taskObj);
            System.out.println("Request sent, waiting for response...");
            DocumentReference result = future.get();
            System.out.println("Task uploaded successfully: " + result.toString());

            return result.toString();
        }
        catch (InterruptedException | ExecutionException e) {
            System.err.println("Could not upload task into Firestore: " + e.getMessage());
            // this indicates a failure.
            return null;
        }
    }

}
