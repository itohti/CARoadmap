package com.caroadmap.api;

import com.caroadmap.dto.GetRecommendationsResponse;
import com.caroadmap.dto.TaskDTO;
import com.caroadmap.dto.TaskFromBossResponse;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Slf4j
public class CARoadmapServer
{
    private static final String SERVER_URL =
            "https://kxin971pll.execute-api.us-east-1.amazonaws.com";

    // private static final String SERVER_URL = "http://localhost:8080";

    private static final MediaType JSON =
            MediaType.get("application/json");

    private static final File pluginDir =
            new File(RuneLite.RUNELITE_DIR, "caroadmap");

    private final OkHttpClient client;
    private final Gson gson;

    @Setter
    @Getter
    private String apiKey;

    @Inject
    private ConfigManager configManager;

    @Inject
    public CARoadmapServer(OkHttpClient client, Gson gson)
    {
        log.info("Initialized CARoadmapServer");
        this.client = client;
        this.gson = gson;
    }

    public boolean storeCharacterData(
            String username,
            long accountHash,
            Map<String, ArrayList<Object>> data)
    {
        log.info("storing character data");

        Map<String, Object> dataToSend = new HashMap<>();
        dataToSend.put("username", username);
        dataToSend.put("accountHash", accountHash);
        dataToSend.put("character_details", data);

        log.info(dataToSend.toString());

        try
        {
            String jsonBody = gson.toJson(dataToSend);

            RequestBody body = RequestBody.create(JSON, jsonBody);

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/characterdata")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                return response.code() == 200;
            }
        }
        catch (Exception e)
        {
            log.error("Could not insert character data.", e);
            return false;
        }
    }

    public boolean updatePlayerBossData(
            long accountHash,
            String bossName,
            int killCount)
    {
        log.info("Updating kill count for boss {}", bossName);

        Map<String, Object> dataToSend = new HashMap<>();
        dataToSend.put("character_id", accountHash);
        dataToSend.put("boss_name", bossName);
        dataToSend.put("kc", killCount);

        try
        {
            String jsonBody = gson.toJson(dataToSend);

            RequestBody body = RequestBody.create(JSON, jsonBody);

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/store_player_boss_data")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                return response.code() == 200;
            }
        }
        catch (Exception e)
        {
            log.error("Could not insert player boss data.", e);
            return false;
        }
    }

    public boolean updatePlayerTaskStatus(
            long accountHash,
            String taskTitle)
    {
        log.info("Updating combat achievement task [{}] to done", taskTitle);

        Map<String, Object> dataToSend = new HashMap<>();
        dataToSend.put("character_id", accountHash);
        dataToSend.put("task_name", taskTitle);
        dataToSend.put("is_done", true);

        try
        {
            String jsonBody = gson.toJson(dataToSend);

            RequestBody body = RequestBody.create(JSON, jsonBody);

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/store_player_task_status")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                return response.code() == 200;
            }
        }
        catch (Exception e)
        {
            log.error("Could not insert player task status.", e);
            return false;
        }
    }

    /**
     * This function will fetch the tasks from the boss.
     *
     * @return an array of tasks from the boss inputted.
     * On error, it will return an EMPTY array.
     */
    public TaskDTO[] fetchTaskFromBoss(String boss, long accountHash)
    {
        Map<String, Object> payload = new HashMap<>();

        payload.put("character_id", accountHash);
        payload.put("boss_name", boss);

        try
        {
            String jsonBody = gson.toJson(payload);

            RequestBody body = RequestBody.create(JSON, jsonBody);

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/get_tasks")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                if (!response.isSuccessful() || response.body() == null)
                {
                    log.error(
                            "Failed to fetch task information for boss {}. Status: {}",
                            boss,
                            response.code()
                    );
                    return new TaskDTO[0];
                }

                TaskFromBossResponse parsedResponse =
                        gson.fromJson(
                                response.body().string(),
                                TaskFromBossResponse.class
                        );

                if (parsedResponse == null || parsedResponse.getTasks() == null)
                {
                    return new TaskDTO[0];
                }

                return parsedResponse.getTasks().toArray(new TaskDTO[0]);
            }
        }
        catch (Exception e)
        {
            log.error(
                    "Could not fetch task information on this boss {}",
                    boss,
                    e
            );
            return new TaskDTO[0];
        }
    }

    public GetRecommendationsResponse getRecommendations(long characterId) throws Exception
    {
        try
        {
            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("character_id", characterId);

            String jsonBody = gson.toJson(dataToSend);

            RequestBody body = RequestBody.create(JSON, jsonBody);

            Request request = new Request.Builder()
                    .url(SERVER_URL + "/get_recommendations")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute())
            {
                if (response.body() == null)
                {
                    log.error(
                            "Failed to get recommendations. Status: {}",
                            response.code()
                    );
                    return null;
                }

                String responseBody = response.body().string();

                if (response.code() != 200)
                {
                    log.error(
                            "Failed to get recommendations. Status: {} Body: {}",
                            response.code(),
                            responseBody
                    );
                    return null;
                }

                return gson.fromJson(
                        responseBody,
                        GetRecommendationsResponse.class
                );
            }
        }
        catch (Exception e)
        {
            log.error("Error fetching recommendations from server", e);
            throw e;
        }
    }
}