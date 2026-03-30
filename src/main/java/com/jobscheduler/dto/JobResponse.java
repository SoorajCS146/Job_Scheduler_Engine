package com.jobscheduler.dto;

import com.jobscheduler.model.JobPriority;
import com.jobscheduler.model.JobState;
import com.jobscheduler.model.JobType;
import lombok.Data;

import java.time.Instant;

/**
 * DTO for job response.
 * Contains job metadata without exposing internal implementation details.
 */
@Data
public class JobResponse {
    
    private int jobId;
    private JobType jobType;
    private JobState jobState;
    private Instant submittedTime;
    private Instant startTime;
    private Instant completedTime;
    private JobPriority jobPriority;
    private Long timeoutSeconds;

}

