package com.caroadmap.dto;

import lombok.Data;

@Data
public class RecommendedTaskDTO {
    private String description;
    private Double estimated_time;
    private Double kills_remaining;
    private String monster;
    private double score;
    private Double seconds_to_save;
    private String task_name;
    private int tier;
    private String type;
}
