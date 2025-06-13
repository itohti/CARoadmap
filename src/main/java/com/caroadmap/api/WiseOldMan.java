package com.caroadmap.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.caroadmap.data.Boss;
import com.caroadmap.dto.EhbResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WiseOldMan {
    private static final String WISE_OLD_MAN_API = "https://api.wiseoldman.net/v2/players/";

    private final String displayName;
    private final HttpClient client;
    private final Gson gson;

    public WiseOldMan(String displayName) {
        this.displayName = displayName;
        this.client = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Fetches boss info from Wise Old Man API.
     * Returns an empty array if any error occurs or data is not found.
     */
    public Boss[] fetchBossInfo() {
        String encodedUsername = URLEncoder.encode(displayName, StandardCharsets.UTF_8).replace("+", "%20");
        String url = WISE_OLD_MAN_API + encodedUsername;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Received non-200 response from Wise Old Man API: {}", response.statusCode());
                return new Boss[0];
            }

            EhbResponse ehbResponse = gson.fromJson(response.body(), EhbResponse.class);

            List<Boss> bossList = new ArrayList<>();
            ehbResponse.latestSnapshot.data.bosses.values().forEach(boss -> {
                String formattedName = formatBossName(boss.metric);
                bossList.add(new Boss(formattedName, boss.kills, boss.ehb));
            });

            return bossList.toArray(new Boss[0]);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to fetch data from Wise Old Man API for user '{}': {}", displayName, e.getMessage());
            return new Boss[0];
        }
    }

    private static String formatBossName(String metric) {
        String[] words = metric.replace("_", " ").split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
                formatted.append(' ');
            }
        }

        return formatted.toString().trim();
    }
}
