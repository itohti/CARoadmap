package com.caroadmap.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Objects;
@Getter
@Setter
public class Task {

    private String boss;
    private String taskName;
    private String taskDescription;
    private TaskType type;
    private int tier;
    private boolean done;

    private Double score;
    private Double completionProbability;
    private Double completionPercent;
    private Double killsRemaining;
    private Double currentKills;
    private Double requiredKills;
    private Boolean hasPb;
    private Double secondsToSave;
    private Double targetTimeSeconds;
    private Double playerTimeSeconds;
    private Double killProgressRatio;

    // Constructor
    public Task(String boss, String taskName, String taskDescription, TaskType type, int tier, boolean done) {
        this.boss = boss;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.type = type;
        this.tier = tier;
        this.done = done;
    }

    /**
     * toString method for Task
     */
    @Override
    public String toString()
    {
        return String.format(
                "Task{name='%s', type=%s, boss='%s', " +
                        "killsRemaining=%s, currentKills=%s, requiredKills=%s, " +
                        "hasPb=%s, targetTime=%s, playerTime=%s, done=%s}",
                taskName,
                type,
                boss,
                killsRemaining,
                currentKills,
                requiredKills,
                hasPb,
                targetTimeSeconds,
                playerTimeSeconds,
                done
        );
    }

    @Override
    public boolean equals(Object obj) {
        // Check for null or if the object is not an instance of Task
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        // Cast the object to a Task
        Task otherTask = (Task) obj;

        // Compare the relevant fields of the Task
        return this.boss.equals(otherTask.boss) &&
                this.taskName.equals(otherTask.taskName) &&
                this.taskDescription.equals(otherTask.taskDescription) &&
                this.type == otherTask.type &&
                this.tier == otherTask.tier &&
                this.done == otherTask.done;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskName);
    }

    public static Comparator<Task> byScore() {
        return Comparator.comparingDouble(Task::getScore);
    }

    public static Comparator<Task> byTier() {
        return Comparator.comparingInt(Task::getTier);
    }

    public static Comparator<Task> byBoss() {
        return Comparator.comparing(Task::getBoss);
    }
}

