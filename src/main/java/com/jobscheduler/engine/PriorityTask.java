package com.jobscheduler.engine;

import com.jobscheduler.model.JobData;

public class PriorityTask implements  Runnable,Comparable<PriorityTask> {

    private final JobData jobData;
    private final Runnable task;

    public PriorityTask( JobData jobData, Runnable task) {
        if(jobData == null || task == null) {
            throw new IllegalArgumentException("JobData and task cannot be null");
        }
        this.jobData = jobData;
        this.task = task;
    }
    public int getJobId() {
        return jobData.getJobId();
    }

    @Override
    public int compareTo(PriorityTask other ){
    int priorityCompare = Integer.compare(this.jobData.getJobPriority().getValue(), other.jobData.getJobPriority().getValue());
    if(priorityCompare != 0){
        return priorityCompare;
    }
    return this.jobData.getSubmittedTime().compareTo(other.jobData.getSubmittedTime());

    }
    @Override
    public void run(){
        task.run();
    }
}
