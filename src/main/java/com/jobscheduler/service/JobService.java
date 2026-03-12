package com.jobscheduler.service;

import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.dto.SubmitJobRequest;
import com.jobscheduler.engine.JobScheduler;
import com.jobscheduler.factory.JobFactory;
import com.jobscheduler.model.JobData;
import com.jobscheduler.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for job operations.
 * Handles business logic and coordinates between controller and engine.
 */
@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobScheduler jobScheduler;

    /**
     * Submit a new job for execution.
     *
     * @param request The job submission request
     * @return The job ID
     */
    public int submitJob(SubmitJobRequest request) {
        // Create JobData from DTO
        JobData jobData = JobFactory.createJobData(request);

        // Save to repository
        int jobId = jobRepository.save(jobData);

        // Schedule for execution
        jobScheduler.scheduleJob(jobId);

        return jobId;
    }

    /**
     * Get a job by ID.
     *
     * @param jobId The job ID
     * @return JobResponse DTO
     */
    public JobResponse getJob(int jobId) {
        JobData jobData = jobRepository.findById(jobId);
        if (jobData == null) {
            return null;
        }
        return JobFactory.toJobResponse(jobData);
    }

    /**
     * Get all jobs.
     *
     * @return List of JobResponse DTOs
     */
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().values().stream()
                .map(JobFactory::toJobResponse)
                .collect(Collectors.toList());
    }
}

