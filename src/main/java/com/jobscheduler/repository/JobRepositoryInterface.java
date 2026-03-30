package com.jobscheduler.repository;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;

import java.util.List;
import java.util.Optional;

public interface JobRepositoryInterface {
    JobData save(JobData jobData);
    Optional<JobData> findByJobId(int jobId);
    List<JobData> findAll();
    boolean existsByJobId(int jobId);
    void deleteByJobId(int jobId);


    List<JobData> findQueuedJobs(int limit);
    void insertAll(List<JobData> jobs);
    List<JobData> findByJobState(JobState jobState);

}
