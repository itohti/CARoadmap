package com.caroadmap.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Data
public class GetRecommendationsResponse {
    public ArrayList<RecommendedTaskDTO> recommended_tasks;
    @Getter
    @Setter
    private String error;
}
