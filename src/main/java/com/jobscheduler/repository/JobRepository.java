package com.jobscheduler.repository;

import com.jobscheduler.model.JobData;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository for job data storage and retrieval.
 * Handles all CRUD operations for jobs.
 */
@Repository
public class JobRepository {
    
    private final Map<Integer, JobData> jobs;
    private final AtomicInteger jobIdCounter = new AtomicInteger(0);
    
    public JobRepository() {
        jobs = new ConcurrentHashMap<>();
    }
    
    /**
     * Save a job and assign it an ID.
     * 
     * @param jobData The job data to save
     * @return The assigned job ID
     */
    public int save(JobData jobData) {
        int jobId = jobIdCounter.incrementAndGet();
        jobData.setJobId(jobId);
        jobs.put(jobId, jobData);
        return jobId;
    }
    
    /**
     * Find a job by ID.
     * 
     * @param jobId The job ID
     * @return The job data, or null if not found
     */
    public JobData findById(int jobId) {
        return jobs.get(jobId);
    }
    
    /**
     * Get all jobs.
     * 
     * @return Map of all jobs
     */
    public Map<Integer, JobData> findAll() {
        return new HashMap<>(jobs);
    }
}

