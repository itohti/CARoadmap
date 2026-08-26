package com.caroadmap.ui;

import java.util.Map;

public class BossNameUtil {
    private static final Map<String, String> DB_ALIASES = Map.ofEntries(
            Map.entry("the leviathan", "leviathan"),
            Map.entry("the whisperer", "whisperer")
    );

    public static String normalizeForDatabase(String boss) {
        String normalized = normalizeBossName(boss);

        return DB_ALIASES.getOrDefault(normalized, normalized);
    }

    public static String normalizeBossName(String metric) {
        return metric
                .toLowerCase()
                .replace("_", " ")
                .replace(":", " ")
                .replace("-", " ")
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
