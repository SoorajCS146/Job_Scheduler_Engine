package com.jobscheduler.dto;

import com.jobscheduler.model.JobPriority;
import com.jobscheduler.model.JobType;

/**
 * DTO for job submission requests.
 * Contains all possible fields for all job types.
 * Only relevant fields will be populated based on jobType.
 */
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


    // Getters and Setters
    public JobType getJobType() {
        return jobType;
    }
    
    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }
    
    public String getReportName() {
        return reportName;
    }
    
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public Integer getRecipientCount() {
        return recipientCount;
    }
    
    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public Integer getOlderThanDays() {
        return olderThanDays;
    }
    
    public void setOlderThanDays(Integer olderThanDays) {
        this.olderThanDays = olderThanDays;
    }
    
    public String getSourceSystem() {
        return sourceSystem;
    }
    
    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }
    
    public String getTargetSystem() {
        return targetSystem;
    }
    
    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }
    
    public Integer getRecordCount() {
        return recordCount;
    }
    
    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public JobPriority getJobPriority() {
        return jobPriority;
    }

    public void setJobPriority(JobPriority jobPriority) {
        this.jobPriority = jobPriority;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

}

