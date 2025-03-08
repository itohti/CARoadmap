package com.caroadmap;

public enum CSVColumns {
    BOSS_NAME(0),
    TASK_NAME(1),
    TASK_DESCRIPTION(2),
    TYPE(3),
    TIER(4),
    DONE(5);

    private final int index;

    CSVColumns(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
