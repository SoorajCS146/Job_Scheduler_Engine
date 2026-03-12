package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataCleanupHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) {
        log.info("🧹 Cleaning table: {}", jobData.getTableName());
        log.info("   Removing records older than {} days", jobData.getOlderThanDays());

        int time = (int)(Math.random() * 4000) + 4000;

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data cleanup interrupted", e);
        }

        log.info("✓ Table '{}' cleaned successfully", jobData.getTableName());
    }
    
    @Override
    public JobType getType() {
        return JobType.DATA_CLEANUP;
    }
}

