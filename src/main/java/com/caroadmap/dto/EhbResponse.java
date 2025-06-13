package com.caroadmap.dto;

import lombok.Data;

import java.util.Map;

@Data
public class EhbResponse {
    public LatestSnapshot latestSnapshot;

    @Data
    public static class LatestSnapshot {
        public SnapshotData data;
    }

    @Data
    public static class SnapshotData {
        public Map<String, Boss> bosses;
    }

    @Data
    public static class Boss {
        public String metric;
        public int kills;
        public int rank;
        public double ehb;
    }
}
