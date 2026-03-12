package com.jobscheduler.dto;

import com.jobscheduler.model.JobState;
import com.jobscheduler.model.JobType;

import java.time.Instant;

/**
 * DTO for job response.
 * Contains job metadata without exposing internal implementation details.
 */
public class JobResponse {
    
    private int jobId;
    private JobType jobType;
    private JobState jobState;
    private Instant submittedTime;
    private Instant startTime;
    private Instant completedTime;
    
    // Getters and Setters
    public int getJobId() {
        return jobId;
    }
    
    public void setJobId(int jobId) {
        this.jobId = jobId;
    }
    
    public JobType getJobType() {
        return jobType;
    }
    
    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
    
    public JobState getJobState() {
        return jobState;
    }
    
    public void setJobState(JobState jobState) {
        this.jobState = jobState;
    }
    
    public Instant getSubmittedTime() {
        return submittedTime;
    }
    
    public void setSubmittedTime(Instant submittedTime) {
        this.submittedTime = submittedTime;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }
}

