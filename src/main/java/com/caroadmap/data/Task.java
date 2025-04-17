package com.caroadmap.data;

import java.util.HashMap;
import java.util.Map;

public class Task {
    private String boss;
    private String taskName;
    private String taskDescription;
    private TaskType type;
    private int tier;
    private boolean done;

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

    // Getter and Setter for boss
    public String getBoss() {
        return boss;
    }

    public void setBoss(String boss) {
        this.boss = boss;
    }

    // Getter and Setter for taskName
    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    // Getter and Setter for taskDescription
    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    // Getter and Setter for type
    public TaskType getType() {
        return type;
    }

    public void setType(int type) {
        this.type = TaskType.fromValue(type);
    }

    // Getter and Setter for tier
    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    // Getter and Setter for done
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
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
}

