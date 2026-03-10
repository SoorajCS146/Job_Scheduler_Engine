package com.jobscheduler.engine;

import com.jobscheduler.job.JobType;
import com.jobscheduler.model.Job;
import com.jobscheduler.model.JobState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JobExecutor {
    private final Map<Integer,Job> jobs;

    private AtomicInteger jobIdCounter = new AtomicInteger(0);
    public JobExecutor()
    {
        jobs = new ConcurrentHashMap<>();
    }
    public JobExecutor(Map<Integer,Job> jobs)
    {
        this.jobs = jobs;
    }

    public Job getJobById(int jobId)
    {
        return jobs.get(jobId);
    }
    public Map<Integer,Job>getAllJobs()
    {
        return new HashMap<>(jobs);
    }
    public int submitJob(Job job){
        int jobId = jobIdCounter.incrementAndGet();
        job.setJobId(jobId);
        jobs.put(jobId, job);
        return jobId;
    }

}
