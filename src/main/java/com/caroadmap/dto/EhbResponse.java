package com.caroadmap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EhbResponse {
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
