package com.jobscheduler.listener;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.ListenerType;

public interface JobEventListener
{
    ListenerType getListenerType();
    boolean shouldHandle(JobData jobData);
    void onJobCompleted(JobData jobData,Exception failureCause);
}
