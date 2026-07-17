package com.caroadmap.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendedTaskDTO extends TaskDTO
{
    private double completion_probability;
    private double score;
}
