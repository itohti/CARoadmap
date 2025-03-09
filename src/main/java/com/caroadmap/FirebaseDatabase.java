package com.caroadmap;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
                    new FileInputStream("./src/main/resources/caroadmap-firebase-adminsdk-fbsvc-9dbd74cf8d.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
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
            ApiFuture<DocumentReference> future = db.collection(collection).add(taskObj);
            DocumentReference result = future.get(10, TimeUnit.SECONDS);

            return result.toString();
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Could not upload task into Firestore: " + e.getMessage());
            // this indicates a failure.
            return null;
        }
    }

}
