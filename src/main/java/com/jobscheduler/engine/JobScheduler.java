package com.jobscheduler.engine;

import com.jobscheduler.model.Job;
import com.jobscheduler.model.JobState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JobScheduler {
    private final ExecutorService executorService;
    private final JobExecutor jobExecutor;

    public JobScheduler(JobExecutor jobExecutor, int poolSize) {
        this.jobExecutor = jobExecutor;
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public JobScheduler(JobExecutor jobExecutor) {
        this(jobExecutor, 5); // Default pool size of 5
    }

    public void scheduleJob(int jobId) {
        // Get the job from JobExecutor
        Job job = jobExecutor.getJobById(jobId);

        if (job == null) {
            throw new IllegalArgumentException("Job not found with ID: " + jobId);
        }

        job.setJobState(JobState.QUEUED);
        executorService.submit(() -> {
            try {
                job.executeJob();

            } catch (Exception e) {
                job.setJobState(JobState.FAILED);
                job.setCompletedTime(java.time.Instant.now());
                System.err.println("Job " + jobId + " failed with exception: " + e.getMessage());
                e.printStackTrace();
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
