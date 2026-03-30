package com.jobscheduler.dto;

import com.jobscheduler.model.JobPriority;
import com.jobscheduler.model.JobType;
import lombok.Data;

/**
 * DTO for job submission requests.
 * Contains all possible fields for all job types.
 * Only relevant fields will be populated based on jobType.
 */
@Data
public class SubmitJobRequest {
    
    private JobType jobType;
    
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

    // Priority fields
    private JobPriority jobPriority;

    //Time out fields
    private Long timeoutSeconds;


}

