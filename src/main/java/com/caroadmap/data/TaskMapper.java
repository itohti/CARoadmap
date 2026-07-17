package com.caroadmap.data;

import com.caroadmap.dto.RecommendedTaskDTO;
import com.caroadmap.dto.TaskDTO;

public class TaskMapper {
    public static TaskType fromDTOString(String type)
    {
        return TaskType.valueOf(type.toUpperCase().replace(" ", "_"));
    }

    public static Task fromDTO(TaskDTO dto)
    {
        Task task = new Task(
                dto.getBoss_name(),
                dto.getTitle(),
                dto.getDescription(),
                fromDTOString(dto.getType()),
                dto.getPoints(),
                false
        );

        task.setCompletionPercent(dto.getCompletion_percent());

        task.setCurrentKills(dto.getCurrent_kills());
        task.setRequiredKills(dto.getRequired_kills());
        task.setKillsRemaining(dto.getKills_remaining());

        task.setHasPb(dto.getHas_pb());

        task.setPlayerTimeSeconds(dto.getPlayer_time_seconds());
        task.setTargetTimeSeconds(dto.getTarget_time_seconds());
        task.setSecondsToSave(dto.getSeconds_to_save());
        task.setKillProgressRatio(dto.getKill_progress_ratio());

        if (dto instanceof RecommendedTaskDTO)
        {
            RecommendedTaskDTO recommended = (RecommendedTaskDTO) dto;

            task.setScore(recommended.getScore());
            task.setCompletionProbability(
                    recommended.getCompletion_probability()
            );
        }

        return task;
    }
}
