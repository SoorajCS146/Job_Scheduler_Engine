package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataCleanupHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) throws InterruptedException {
        log.info("🧹 Cleaning table: {}", jobData.getTableName());
        log.info("   Removing records older than {} days", jobData.getOlderThanDays());

        int time = (int)(Math.random() * 4000) + 4000;

        // Interruptible sleep - check every 100ms
        int iterations = time / 100;
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Data cleanup cancelled");
            }
            Thread.sleep(100);
        }

        log.info("✓ Table '{}' cleaned successfully", jobData.getTableName());
    }
    
    @Override
    public JobType getType() {
        return JobType.DATA_CLEANUP;
    }
}

