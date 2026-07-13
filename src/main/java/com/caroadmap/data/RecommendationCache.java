package com.caroadmap.data;

import com.caroadmap.dto.RecommendedTaskDTO;
import lombok.Data;

import java.util.ArrayList;

@Data
public class RecommendationCache {

    private long characterId;

    private String generatedAt;

    private ArrayList<RecommendedTaskDTO> recommendedTasks;
}
