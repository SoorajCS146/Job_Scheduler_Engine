package com.jobscheduler.listener;


import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;
import com.jobscheduler.model.ListenerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;


@Slf4j
@Component
public class AuditLogListener implements  JobEventListener {

    @Override
    public ListenerType getListenerType() {
        return ListenerType.AUDIT_LOG;
    }

    @Override
    public boolean shouldHandle(JobData jobData) {
        return true;
    }

    @Override
    public void onJobCompleted(JobData jobData,Exception failureCause) {
        log.info("\n" + "=".repeat(60));
        log.info("📋 AUDIT LOG");
        log.info("=".repeat(60));
        log.info("Job ID:          {}", jobData.getJobId());
        log.info("Job Type:        {}", jobData.getJobType());
        log.info("Job State:       {}", jobData.getJobState());
        log.info("Submitted Time:  {}", jobData.getSubmittedTime());
        log.info("Start Time:      {}", jobData.getStartTime());
        log.info("Completed Time:  {}", jobData.getCompletedTime());
        log.info("Execution Time:  {}ms", Duration.between(jobData.getStartTime(), jobData.getCompletedTime()).toMillis());

        if(jobData.getJobState() == JobState.FAILED) {
            log.info("Failure Reason:  {}", failureCause.getMessage());
        }
        log.info("=".repeat(60) + "\n");
    }
}

