package com.caroadmap.dto;

import lombok.Data;

@Data
public class TaskDTO {
    private String boss_name;
    private String title;
    private String description;
    private int points;
    private double completion_percent;
    private String type;

    public String toString() {
        return String.format("Boss %s, Title %s, Description: %s, Points: %d, Completion Percent: %f, Type: %s",
                boss_name,
                title,
                description,
                points,
                completion_percent,
                type);
    }
}
