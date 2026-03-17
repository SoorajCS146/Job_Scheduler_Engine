package com.jobscheduler.model;

public enum JobPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
