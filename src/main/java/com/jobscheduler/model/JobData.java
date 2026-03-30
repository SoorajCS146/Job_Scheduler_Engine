package com.jobscheduler.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
@Data
@NoArgsConstructor

@Document(collection = "test_jobs")
public class JobData {
    // Core metadata
    @Id
    private String id;
    private int jobId;
    private JobType jobType;
    private JobState jobState;
    private Instant submittedTime;
    private Instant startTime;
    private Instant completedTime;

    //Priority fields
    private JobPriority jobPriority;

    //Time out fields
    private Long timeoutSeconds;


    // ReportGeneration fields
    private String reportName;
    private String department;
    
    // EmailNotification fields
    private String subject;
    private Integer recipientCount;
    
    // DataCleanup fields
    private String tableName;
    private Integer olderThanDays;
    
    // DataSync fields
    private String sourceSystem;
    private String targetSystem;
    private Integer recordCount;



    // Constructor
    public JobData(JobType jobType) {
        this.jobType = jobType;
        this.jobState = JobState.SUBMITTED;
        this.submittedTime = Instant.now();
        this.jobPriority = JobPriority.MEDIUM;
        this.timeoutSeconds = 30L;
    }

}

