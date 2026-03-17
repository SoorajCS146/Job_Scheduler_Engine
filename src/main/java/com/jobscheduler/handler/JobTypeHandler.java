package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;

/**
 * Interface for all job type handlers.
 * Each handler implements the execution logic for a specific job type.
 */
public interface JobTypeHandler {
    
    /**
     * Execute the job using the provided job data
     * @param jobData The job data containing all necessary fields
     * @throws InterruptedException if the job is cancelled during execution
     */
    void execute(JobData jobData) throws InterruptedException;
    
    /**
     * Get the job type this handler supports
     * @return The JobType enum value
     */
    JobType getType();
}

