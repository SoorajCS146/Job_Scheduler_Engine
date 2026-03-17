package com.jobscheduler.engine;

import java.util.concurrent.FutureTask;

public class PriorityFutureTask<V> extends FutureTask implements Comparable<PriorityFutureTask<V>> {
    private final PriorityTask priorityTask;

    public PriorityFutureTask(PriorityTask priorityTask,V result) {
        super(priorityTask, result);
        this.priorityTask = priorityTask;
    }

    @Override
    public int compareTo(PriorityFutureTask<V> o) {
        return this.priorityTask.compareTo(o.priorityTask);
    }

    public int getJobId() {
        return priorityTask.getJobId();
    }
}


