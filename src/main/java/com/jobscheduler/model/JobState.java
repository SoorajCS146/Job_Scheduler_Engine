package com.jobscheduler.model;

public enum JobState {
    SUBMITTED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMED_OUT
}
