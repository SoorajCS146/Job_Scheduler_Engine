package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportGenerationHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) throws InterruptedException {
        log.info("📊 Generating Report: {}", jobData.getReportName());
        log.info("   Department: {}", jobData.getDepartment());

        // Simulate report generation (3-6 seconds) - interruptible
        int totalTime = 3000 + (int)(Math.random() * 3000);
        int iterations = totalTime / 100;

        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Report generation cancelled");
            }
            Thread.sleep(100);
        }

        log.info("✓ Report '{}' generated for {} department", jobData.getReportName(), jobData.getDepartment());
    }
    
    @Override
    public JobType getType() {
        return JobType.REPORT_GENERATION;
    }
}

