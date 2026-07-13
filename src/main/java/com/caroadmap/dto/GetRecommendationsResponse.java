package com.caroadmap.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Data
public class GetRecommendationsResponse {

    @SerializedName("recommended_tasks")
    private ArrayList<RecommendedTaskDTO> recommendedTasks;

    @SerializedName("generated_at")
    private String generatedAt;

    private String error;
}
