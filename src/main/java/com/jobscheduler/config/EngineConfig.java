package com.jobscheduler.config;

import com.jobscheduler.engine.JobExecutor;
import com.jobscheduler.engine.JobScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public JobExecutor jobExecutor() {
        return new JobExecutor();
    }

    @Bean
    public JobScheduler jobScheduler(JobExecutor jobExecutor) {
        return new JobScheduler(jobExecutor);
    }
}