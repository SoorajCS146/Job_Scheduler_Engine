package com.jobscheduler.factory;

import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.dto.SubmitJobRequest;
import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;

/**
 * Factory for creating JobData from DTOs.
 * Centralizes the job creation logic and field mapping.
 */
public class JobFactory {
    
    /**
     * Create JobData from SubmitJobRequest DTO.
     * Maps only the relevant fields based on job type.
     * 
     * @param request The job submission request
     * @return JobData with populated fields
     */
    public static JobData createJobData(SubmitJobRequest request) {
        JobData jobData = new JobData(request.getJobType());
        
        // Map fields based on job type
        switch (request.getJobType()) {
            case REPORT_GENERATION:
                jobData.setReportName(request.getReportName());
                jobData.setDepartment(request.getDepartment());
                break;
                
            case EMAIL_NOTIFICATION:
                jobData.setSubject(request.getSubject());
                jobData.setRecipientCount(request.getRecipientCount());
                break;
                
            case DATA_CLEANUP:
                jobData.setTableName(request.getTableName());
                jobData.setOlderThanDays(request.getOlderThanDays());
                break;
                
            case DATA_SYNC:
                jobData.setSourceSystem(request.getSourceSystem());
                jobData.setTargetSystem(request.getTargetSystem());
                jobData.setRecordCount(request.getRecordCount());
                break;
                
            default:
                throw new IllegalArgumentException("Unknown job type: " + request.getJobType());
        }
        
        return jobData;
    }
    
    /**
     * Convert JobData to JobResponse DTO.
     * 
     * @param jobData The job data
     * @return JobResponse DTO
     */
    public static JobResponse toJobResponse(JobData jobData) {
        JobResponse response = new JobResponse();
        response.setJobId(jobData.getJobId());
        response.setJobType(jobData.getJobType());
        response.setJobState(jobData.getJobState());
        response.setSubmittedTime(jobData.getSubmittedTime());
        response.setStartTime(jobData.getStartTime());
        response.setCompletedTime(jobData.getCompletedTime());
        return response;
    }
}

