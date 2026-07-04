package com.caroadmap.data;

import com.caroadmap.dto.TaskDTO;

public class TaskMapper {
    public static TaskType fromDTOString(String type)
    {
        return TaskType.valueOf(type.toUpperCase().replace(" ", "_"));
    }

    public static Task fromDTO(TaskDTO dto)
    {
        return new Task(
                dto.getBoss_name(),
                dto.getTitle(),
                dto.getDescription(),
                fromDTOString(dto.getType()),
                dto.getPoints(),
                false
        );
    }
}
