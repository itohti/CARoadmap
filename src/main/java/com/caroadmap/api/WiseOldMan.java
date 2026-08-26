package com.caroadmap.api;

import com.caroadmap.data.Boss;
import com.caroadmap.dto.EhbResponse;
import com.caroadmap.ui.BossNameUtil;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WiseOldMan
{
    private static final String WISE_OLD_MAN_API = "https://api.wiseoldman.net/v2/players/";

    private final OkHttpClient client;
    private final Gson gson;

    @Inject
    public WiseOldMan(OkHttpClient client, Gson gson)
    {
        this.client = client;
        this.gson = gson;
    }

    /**
     * Fetches boss info from Wise Old Man API.
     * Returns an empty array if any error occurs or data is not found.
     */
    public Boss[] fetchBossInfo(String displayName)
    {
        HttpUrl url = HttpUrl.parse(WISE_OLD_MAN_API + displayName);

        if (url == null)
        {
            log.warn("Failed to create Wise Old Man URL for user '{}'", displayName);
            return new Boss[0];
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                log.warn(
                        "Received non-success response from Wise Old Man API for user '{}': {}",
                        displayName,
                        response.code()
                );
                return new Boss[0];
            }

            if (response.body() == null)
            {
                log.warn("Wise Old Man API returned an empty response for user '{}'", displayName);
                return new Boss[0];
            }

            EhbResponse ehbResponse = gson.fromJson(
                    response.body().string(),
                    EhbResponse.class
            );

            if (ehbResponse == null
                    || ehbResponse.latestSnapshot == null
                    || ehbResponse.latestSnapshot.data == null
                    || ehbResponse.latestSnapshot.data.bosses == null)
            {
                log.warn("Wise Old Man API returned no boss data for user '{}'", displayName);
                return new Boss[0];
            }

            List<Boss> bossList = new ArrayList<>();

            ehbResponse.latestSnapshot.data.bosses.values().forEach(boss ->
            {
                String normalized = BossNameUtil.normalizeForDatabase(boss.metric);
                bossList.add(new Boss(normalized, boss.kills, boss.ehb));
            });

            return bossList.toArray(new Boss[0]);
        }
        catch (IOException e)
        {
            log.error(
                    "Failed to fetch data from Wise Old Man API for user '{}'",
                    displayName,
                    e
            );
            return new Boss[0];
        }
        catch (Exception e)
        {
            log.error(
                    "Failed to parse Wise Old Man API response for user '{}'",
                    displayName,
                    e
            );
            return new Boss[0];
        }
    }
}