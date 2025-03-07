package com.caroadmap;

public enum ExcelColumns {
    BOSS_NAME(0),
    TASK_NAME(1),
    TASK_DESCRIPTION(2),
    TYPE(3),
    TIER(4),
    DONE(5);

    private final int index;

    ExcelColumns(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
