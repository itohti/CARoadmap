package com.caroadmap.data;

public enum SortingType {
    BOSS(1),
    SCORE(2),
    TIER(3);
    private final int value;

    SortingType(int value) { this.value = value; }
}
