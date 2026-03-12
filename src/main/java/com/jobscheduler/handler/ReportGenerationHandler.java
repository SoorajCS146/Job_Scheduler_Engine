package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportGenerationHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) {
        log.info("📊 Generating Report: {}", jobData.getReportName());
        log.info("   Department: {}", jobData.getDepartment());

        try {
            // Simulate report generation (3-6 seconds)
            Thread.sleep(3000 + (int)(Math.random() * 3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report generation interrupted", e);
        }

        log.info("✓ Report '{}' generated for {} department", jobData.getReportName(), jobData.getDepartment());
    }
    
    @Override
    public JobType getType() {
        return JobType.REPORT_GENERATION;
    }
}

