package com.jobscheduler.service;

import com.jobscheduler.model.JobData;
import com.jobscheduler.repository.JobRepositoryInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobIdGenerator {

    private final AtomicInteger counter;
    private final JobRepositoryInterface jobRepository;

    @Autowired
    public JobIdGenerator(JobRepositoryInterface jobRepository) {
        this.jobRepository = jobRepository;
        int maxJobId = getMaxJobIdFromDatabase();
        this.counter = new AtomicInteger(maxJobId);
    }

    public int generateJobId() {
        return counter.incrementAndGet();
    }

    private int getMaxJobIdFromDatabase() {
        // Find all jobs and get max jobId
        return jobRepository.findAll().stream()
                .mapToInt(JobData::getJobId)
                .max()
                .orElse(0);
    }
}