package com.jobscheduler.mediator;

import com.jobscheduler.listener.JobEventListener;
import com.jobscheduler.model.JobData;
import com.jobscheduler.model.ListenerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class JobEventMediatorImpl implements  JobEventMediator {
    Map<ListenerType, JobEventListener> listenerMap;
    public JobEventMediatorImpl(Map<ListenerType, JobEventListener> listenerMap) {
        this.listenerMap = listenerMap;
    }
    public JobEventMediatorImpl() {
        this.listenerMap = new ConcurrentHashMap<>();
    }
    public void registerListener( JobEventListener jobEventListener) {
        ListenerType listenerType = jobEventListener.getListenerType();
        listenerMap.put(listenerType, jobEventListener);
    }
    public void unregisterListener(ListenerType listenerType) {
        listenerMap.remove(listenerType);
    }
    public void notifyListeners(JobData jobData, Exception failureCause) {
        for (JobEventListener listener : listenerMap.values()) {
            try{
                if(listener.shouldHandle(jobData)) {
                    listener.onJobCompleted(jobData, failureCause);
                }
            }
            catch (Exception e) {
                log.error("Listener {} failed: {}", listener.getListenerType(), e.getMessage(), e);
            }

        }
    }
}
