package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataSyncHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) throws InterruptedException {
        int time = Math.max(2 * jobData.getRecordCount(), 6000);

        log.info("🔄 Syncing {} records", jobData.getRecordCount());
        log.info("   From: {}", jobData.getSourceSystem());
        log.info("   To: {}", jobData.getTargetSystem());

        // Interruptible sleep - check every 100ms
        int iterations = time / 100;
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Data sync cancelled");
            }
            Thread.sleep(100);
        }

        log.info("✓ Data sync completed successfully");
    }
    
    @Override
    public JobType getType() {
        return JobType.DATA_SYNC;
    }
}

