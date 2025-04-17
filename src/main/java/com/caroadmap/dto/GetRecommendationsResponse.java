package com.caroadmap.dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class GetRecommendationsResponse {
    public ArrayList<RecommendedTaskDTO> recommended_tasks;
}
