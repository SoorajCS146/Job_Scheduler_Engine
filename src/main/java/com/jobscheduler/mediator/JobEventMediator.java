package com.jobscheduler.mediator;

import com.jobscheduler.listener.JobEventListener;
import com.jobscheduler.model.JobData;
import com.jobscheduler.model.ListenerType;

public interface JobEventMediator
{
    void registerListener( JobEventListener jobEventListener);
    void unregisterListener(ListenerType listenerType);
    void notifyListeners(JobData jobData, Exception failureCause);
}
