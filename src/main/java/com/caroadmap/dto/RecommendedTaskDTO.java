package com.caroadmap.dto;

import lombok.Data;

@Data
public class RecommendedTaskDTO {

    private String title;
    private String description;
    private String boss_name;
    private String type;

    private int points;
    private double score;

    private double completion_probability;
    private double completion_percent;

    private Double current_kills;
    private Double required_kills;
    private Double kills_remaining;
    private Double kill_progress_ratio;

    private boolean has_pb;

    private Double player_time_seconds;
    private Double target_time_seconds;
    private Double seconds_to_save;

    private Double required_slayer;
    private Double slayer_gap;
}
