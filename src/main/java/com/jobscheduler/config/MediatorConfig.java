package com.jobscheduler.config;

import com.jobscheduler.listener.JobEventListener;
import com.jobscheduler.mediator.JobEventMediator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MediatorConfig {
    private final JobEventMediator jobEventMediator;
    private final List<JobEventListener> listeners;

    @Autowired
    public MediatorConfig(JobEventMediator jobEventMediator, List<JobEventListener> listeners) {
        this.jobEventMediator = jobEventMediator;
        this.listeners = listeners;
    }
    @PostConstruct
    public void registerListeners() {
        for (JobEventListener listener : listeners) {
            jobEventMediator.registerListener(listener);
        }
    }
}
