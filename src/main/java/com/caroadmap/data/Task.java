package com.caroadmap.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Task {
    @Getter
    @Setter
    private String boss;
    @Getter
    @Setter
    private String taskName;
    @Getter
    @Setter
    private String taskDescription;
    @Getter
    private TaskType type;
    @Getter
    @Setter
    private int tier;
    @Getter
    @Setter
    private boolean done;
    @Getter
    @Setter
    private Double score;

    // Constructor
    public Task(String boss, String taskName, String taskDescription, TaskType type, int tier, boolean done) {
        this.boss = boss;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.type = type;
        this.tier = tier;
        this.done = done;
    }

    public Task(String boss, String taskName, String taskDescription, String type, String tier, String done) {
        this.boss = boss;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.type = TaskType.valueOf(type);
        this.tier = Integer.parseInt(tier);
        this.done = Boolean.parseBoolean(done);
    }

    public void setType(int type) {
        this.type = TaskType.fromValue(type);
    }

    /**
     * This method is used to convert the Task object into a Map<String, Object> for Firestore.
     * @return a Map<String, Object> where the variable name maps to the variable object.
     */
    public Map<String, Object> formatTask() {
        Map<String, Object> task = new HashMap<>();
        task.put("Boss", boss);
        task.put("Task Name", taskName);
        task.put("Task Description", taskDescription);
        task.put("Type", type.name());
        task.put("Tier", tier);
        task.put("Done", done);

        return task;
    }

    /**
     * toString method for Task
     * @return boss, taskName, taskDescription, type, tier, done
     */
    public String toString() {
        // Escape any quotes in all fields by doubling them
        String escapedBoss = boss.replace("\"", "\"\"");
        String escapedTaskName = taskName.replace("\"", "\"\"");
        String escapedDescription = taskDescription.replace("\"", "\"\"");

        return String.format("%s,\"%s\",\"%s\",%s,%d,%b",
                escapedBoss,
                escapedTaskName,
                escapedDescription,
                type.name(),
                tier,
                done);
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

