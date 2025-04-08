package com.caroadmap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

public class WiseOldMan {
    private final String displayName;
    private final HttpClient client;

    public WiseOldMan(String displayName) {
        this.client = HttpClient.newHttpClient();
        this.displayName = displayName;
    }

    /**
     * This will fetch the boss info from wise old man.
     * @return returns an array of objects but if it fails it will not throw an error rather it will just return an empty array.
     */
    public Boss[] fetchBossInfo() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wiseoldman.net/v2/players/" + displayName))
                .GET()
                .build();
        ArrayList<Boss> bossList = new ArrayList<>();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                EhbResponse ehbResponse = mapper.readValue(response.body(), EhbResponse.class);

                for (EhbResponse.Boss boss : ehbResponse.latestSnapshot.data.bosses.values()) {
                    String formattedString = boss.metric.replace("_", " ");
                    StringBuilder formattedStringBuilder = getStringBuilder(formattedString);

                    formattedString = formattedStringBuilder.toString().trim();
                    bossList.add(new Boss(formattedString, boss.kills, boss.ehb));
                }
                return bossList.toArray(new Boss[0]);
            }
            else {
                return bossList.toArray(new Boss[0]);
            }
        }
        catch (IOException | InterruptedException e) {
            System.err.println("Could not fetch wise old man data: " + e);
            return bossList.toArray(new Boss[0]);
        }
    }

    private static StringBuilder getStringBuilder(String formattedString) {
        String[] words = formattedString.split(" ");
        StringBuilder formattedStringBuilder = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formattedStringBuilder.append(word.substring(0, 1).toUpperCase());
                formattedStringBuilder.append(word.substring(1).toLowerCase());
                formattedStringBuilder.append(" ");
            }
        }
        return formattedStringBuilder;
    }
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class EhbResponse {
    public LatestSnapshot latestSnapshot;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatestSnapshot {
        public SnapshotData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnapshotData {
        public Map<String, Boss> bosses;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Boss {
        public String metric;
        public int kills;
        public int rank;
        public double ehb;
    }
}
