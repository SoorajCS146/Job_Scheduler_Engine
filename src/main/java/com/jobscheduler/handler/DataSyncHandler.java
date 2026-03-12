package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataSyncHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) {
        int time = Math.max(2 * jobData.getRecordCount(), 6000);

        log.info("🔄 Syncing {} records", jobData.getRecordCount());
        log.info("   From: {}", jobData.getSourceSystem());
        log.info("   To: {}", jobData.getTargetSystem());

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Data sync interrupted", e);
        }

        log.info("✓ Data sync completed successfully");
    }
    
    @Override
    public JobType getType() {
        return JobType.DATA_SYNC;
    }
}

