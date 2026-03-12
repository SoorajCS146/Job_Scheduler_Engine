package com.jobscheduler.engine;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;
import com.jobscheduler.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JobScheduler manages the thread pool for job execution.
 * Delegates actual execution to JobExecutor.
 */
@Slf4j
@Component
public class JobScheduler {

    private final ExecutorService executorService;

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private JobRepository jobRepository;

    public JobScheduler() {
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public JobScheduler(JobExecutor jobExecutor, int poolSize) {
        this.jobExecutor = jobExecutor;
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public JobScheduler(JobExecutor jobExecutor) {
        this(jobExecutor, 5); // Default pool size of 5
    }

    /**
     * Schedule a job for execution.
     */
    public void scheduleJob(int jobId) {
        JobData jobData = jobRepository.findById(jobId);

        if (jobData == null) {
            throw new IllegalArgumentException("Job not found with ID: " + jobId);
        }

        jobData.setJobState(JobState.QUEUED);

        executorService.submit(() -> {
            try {
                // Delegate to JobExecutor which uses the registry
                jobExecutor.execute(jobId);
            } catch (Exception e) {
                jobData.setJobState(JobState.FAILED);
                jobData.setCompletedTime(java.time.Instant.now());
                log.error("Job {} failed with exception: {}", jobId, e.getMessage(), e);
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }
}
