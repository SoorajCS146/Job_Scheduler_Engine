package com.jobscheduler.listener;


import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;
import com.jobscheduler.model.ListenerType;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Component
@Slf4j
public class AlertingListener implements JobEventListener {

    @Override
    public ListenerType getListenerType() {
        return ListenerType.ALERTING;
    }
    @Override
    public boolean shouldHandle(JobData jobData) {
        return true;
    }
    @Override
    public void onJobCompleted(JobData jobData,Exception failureCause) {
        long executionTimeMs = Duration.between(jobData.getStartTime(), jobData.getCompletedTime()).toMillis();
        if(jobData.getJobState() == JobState.FAILED){
            log.error("❌ FAILURE: Job #{} ({}) failed after {}ms - Reason: {}",
                    jobData.getJobId(),
                    jobData.getJobType(),
                    executionTimeMs,
                    failureCause.getMessage());
        }
        else{

           log.info("✅ SUCCESS: Job #{} ({}) completed in {}ms",
                   jobData.getJobId(),
                   jobData.getJobType(),
                   executionTimeMs);
        }
    }
}

