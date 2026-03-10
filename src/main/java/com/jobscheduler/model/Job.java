package com.jobscheduler.model;

import com.jobscheduler.job.JobType;
import java.time.Instant;

public class Job {
    private int jobId;
    private JobType jobType;
    private JobState jobState;
    private Instant submittedTime;
    private Instant completedTime;
    private Instant startTime;

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jb) {
        this.jobType = jb;
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

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Job(JobType jb) {
        this.jobType = jb;
        this.jobState = JobState.SUBMITTED;  // Using enum instead of String
        this.submittedTime = Instant.now();
    }

    public void executeJob() {
        try{
            this.setStartTime(Instant.now());
            this.setJobState(JobState.RUNNING);
            this.jobType.runJob();
            this.setJobState(JobState.COMPLETED);
        }
        catch (Exception e){
            this.setJobState(JobState.FAILED);
        }
        finally {
            this.setCompletedTime(Instant.now());
        }

    }
}
