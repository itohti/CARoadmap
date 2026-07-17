package com.caroadmap.dto;

import lombok.Data;

@Data
public class TaskDTO
{
    private int task_id;

    private String boss_name;
    private String title;
    private String description;
    private String type;

    private int points;
    private double completion_percent;

    // Kill Count
    private Double current_kills;
    private Double required_kills;
    private Double kills_remaining;
    private Double kill_progress_ratio;

    // Speed
    private Boolean has_pb;
    private Double player_time_seconds;
    private Double target_time_seconds;
    private Double seconds_to_save;

    // Slayer
    private Double required_slayer;
    private Double slayer_gap;
}
