package com.jobscheduler.service;

import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.dto.SubmitJobRequest;
import com.jobscheduler.engine.JobScheduler;
import com.jobscheduler.factory.JobFactory;
import com.jobscheduler.model.JobData;
//import com.jobscheduler.repository.JobRepository;
import com.jobscheduler.repository.JobRepositoryInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for job operations.
 * Handles business logic and coordinates between controller and engine.
 */
@Service
public class JobService {

    @Autowired
    private JobRepositoryInterface jobRepository;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobIdGenerator jobIdGenerator;

    /**
     * Submit a new job for execution.
     *
     * @param request The job submission request
     * @return The job ID
     */
    public int submitJob(SubmitJobRequest request) {
        // Create JobData from DTO
        JobData jobData = JobFactory.createJobData(request);

        // Save to repository and get assigned ID
        int jobId = jobIdGenerator.generateJobId();
        jobData.setJobId(jobId);
        jobRepository.save(jobData);

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
        Optional<JobData> jobData = jobRepository.findByJobId(jobId);
        return jobData.map(JobFactory::toJobResponse).orElse(null);
    }

    /**
     * Get all jobs.
     *
     * @return List of JobResponse DTOs
     */
    public List<JobResponse> getAllJobs() {
        return  jobRepository.findAll().stream()
                .map(JobFactory::toJobResponse)
                .toList();
    }

    /**
     * Cancel a running or queued job.
     *
     * @param jobId The job ID
     */
    public void cancelJob(int jobId) {
        jobScheduler.cancelJob(jobId);
    }
}

