package com.caroadmap.data;

import com.caroadmap.dto.RecommendedTaskDTO;
import lombok.Data;

import java.util.ArrayList;

@Data
public class RecommendationCache {

    private long characterId;

    private String generatedAt;

    /** Knapsack point target this cache was generated for; null = server default (hard cap of 20). */
    private Integer pointsNeeded;

    private ArrayList<RecommendedTaskDTO> recommendedTasks;
}
