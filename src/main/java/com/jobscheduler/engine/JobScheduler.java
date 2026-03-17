package com.jobscheduler.engine;

import com.jobscheduler.exception.ConflictException;
import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobState;
import com.jobscheduler.repository.JobRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;

/**
 * JobScheduler manages the thread pool for job execution.
 * Delegates actual execution to JobExecutor.
 */
@Slf4j
@Component
public class JobScheduler {

    private final ThreadPoolExecutor executorService;
    private final PriorityBlockingQueue<Runnable> priorityQueue;
    private final ConcurrentHashMap<Integer, Future<?>> runningJobs;
    private final ConcurrentHashMap<Integer, PriorityTask> queuedTasks;

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TimeoutManager timeoutManager;

    public JobScheduler(@Value("${job.scheduler.pool.size:2}") int poolSize) {
        this.priorityQueue = new PriorityBlockingQueue<>(1000);
        this.runningJobs = new ConcurrentHashMap<>();
        this.queuedTasks = new ConcurrentHashMap<>();
        this.executorService = new ThreadPoolExecutor(
                poolSize,  // corePoolSize
                poolSize,  // maximumPoolSize
                0L,        // keepAliveTime
                TimeUnit.MILLISECONDS,
                this.priorityQueue
        ){

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                if(runnable instanceof PriorityTask) {
                    return (RunnableFuture<T>)new PriorityFutureTask<>((PriorityTask)runnable, value);
                }
                return super.newTaskFor(runnable, value);
            }
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                if (r instanceof PriorityFutureTask<?> task) {
                    queuedTasks.remove(task.getJobId());
                    runningJobs.put(task.getJobId(), task);

                    JobData jobData = jobRepository.findById(task.getJobId());
                    if(jobData != null) {
                        long timeoutSeconds = jobData.getTimeoutSeconds() != null ? jobData.getTimeoutSeconds() : 30L;
                        timeoutManager.scheduleTimeout(task.getJobId(), timeoutSeconds, () -> {
                           handleTimeout(task.getJobId());
                           log.debug("Scheduled timeout for job {} ({}s)",task.getJobId(), timeoutSeconds);
                        });
                    }
                }
            }
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (r instanceof PriorityFutureTask<?> task) {

                    int jobId = task.getJobId();
                    boolean timeoutCancelled = timeoutManager.cancelTimeout(jobId);
                    if(timeoutCancelled) {
                        log.debug("Cancelled timeout for job {}", jobId);
                    }
                    runningJobs.remove(jobId);
                }
            }
        };
    }


/**
     * Schedule a job for execution.
     */
    public void scheduleJob(int jobId) {
        JobData jobData = jobRepository.findById(jobId);
        if (jobData == null) {
            throw new IllegalArgumentException("Job not found with ID: " + jobId);
        }

        jobData.setJobState(JobState.QUEUED);

        // Wrap task in PriorityTask
        PriorityTask priorityTask = new PriorityTask(jobData, () -> {
            try {
                jobExecutor.execute(jobId);
            } catch (Exception e) {
                jobData.setJobState(JobState.FAILED);
                jobData.setCompletedTime(java.time.Instant.now());
                log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            }
        });
        queuedTasks.put(jobId, priorityTask);

        executorService.submit(priorityTask);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down JobScheduler...");
        timeoutManager.shutdown();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("JobScheduler shutdown complete");
    }

    public boolean cancelJob(int jobId) {
        JobData job = jobRepository.findById(jobId);

        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        if (job.getJobState() == JobState.COMPLETED ||
                job.getJobState() == JobState.FAILED ||
                job.getJobState() == JobState.CANCELLED ||
                job.getJobState() == JobState.TIMED_OUT) {
            throw new ConflictException("Cannot cancel job in state: " + job.getJobState());
        }

        if (job.getJobState() == JobState.QUEUED) {
            PriorityTask task = queuedTasks.remove(jobId);
            if (task != null) {
                priorityQueue.remove(task);
            }
            job.setJobState(JobState.CANCELLED);
            job.setCompletedTime(java.time.Instant.now());
            log.info("Cancelled queued job {}", jobId);
            return true;
        }

        if (job.getJobState() == JobState.RUNNING) {
            Future<?> future = runningJobs.get(jobId);
            if (future != null) {
                future.cancel(true);  // ← This interrupts the thread!
            }
            job.setJobState(JobState.CANCELLED);
            job.setCompletedTime(java.time.Instant.now());
            log.info("Cancelled running job {}", jobId);
            return true;
        }

        return false;
    }
    private void handleTimeout(int jobId) {
        JobData job = jobRepository.findById(jobId);
        if(job!=null && job.getJobState() == JobState.RUNNING) {
            log.warn("Job {} timed out after {}s",jobId,job.getTimeoutSeconds());

            Future<?> future = runningJobs.get(jobId);
            if(future != null) {
                future.cancel(true);
                log.debug("Interrupted job {} due to timeout ",jobId);
            }
            job.setJobState(JobState.TIMED_OUT);
            job.setCompletedTime(java.time.Instant.now());
            log.info("Marked job {} as TIMED_OUT", jobId);
        }
        else{
            if(job == null) {
                log.debug("Timeout fired for job{} but state is already {}",jobId,job.getJobState());
            }
        }
    }
    public boolean isShutdown() {
        return executorService.isShutdown();
    }
}
