package com.jobscheduler.engine;

import com.jobscheduler.handler.JobTypeHandler;
import com.jobscheduler.mediator.JobEventMediator;
import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;
import com.jobscheduler.registry.JobRegistry;
import com.jobscheduler.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * JobExecutor is responsible for EXECUTING jobs.
 * Uses JobRegistry to fetch the appropriate handler - NO SWITCH STATEMENT!
 * Storage is handled by JobRepository (Single Responsibility Principle).
 * Event notifications are handled by JobEventMediator.
 */
@Slf4j
@Component
public class JobExecutor {

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobEventMediator jobEventMediator;
    /**
     * Execute a job using the registry pattern.
     * NO SWITCH STATEMENT - uses polymorphism via registry!
     *Publishes event to mediator on completion
     * @param jobId The ID of the job to execute
     */
    public void execute(int jobId) {
        JobData jobData = jobRepository.findById(jobId);
        if (jobData == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        Exception failureCause = null;

        try {
            jobData.setStartTime(Instant.now());
            jobData.setJobState(JobState.RUNNING);

            // Fetch handler from registry - NO SWITCH!
            JobTypeHandler handler = jobRegistry.getHandler(jobData.getJobType());

            // Execute using polymorphism
            handler.execute(jobData);

            jobData.setJobState(JobState.COMPLETED);

        }
        catch (InterruptedException e) {
            if(jobData.getJobState() == JobState.TIMED_OUT){
                log.warn("Job {} was interrupted as it timed out", jobId);
            }
            else{
                jobData.setJobState(JobState.CANCELLED);
                log.warn("Job {} was cancelled", jobId);
            }
            failureCause = e;
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            jobData.setJobState(JobState.FAILED);
            failureCause = e;
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
        }
        finally {
            jobData.setCompletedTime(Instant.now());

            // Publish event - listeners will be notified automatically!
            jobEventMediator.notifyListeners(jobData, failureCause);
        }
    }
}
