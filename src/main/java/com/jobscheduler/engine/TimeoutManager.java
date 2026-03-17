package com.jobscheduler.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;


@Slf4j
@Component
public class TimeoutManager {

    private final ScheduledExecutorService timeoutScheduler;
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> timeoutTasks;

    public TimeoutManager() {
        // Single thread is sufficient for timeout scheduling
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "timeout-scheduler");
            thread.setDaemon(true);  // Don't prevent JVM shutdown
            return thread;
        });
        this.timeoutTasks = new ConcurrentHashMap<>();
        log.info("✓ TimeoutManager initialized");
    }

    /**
     * Schedule a timeout for a job.
     * If the timeout fires, the provided onTimeout callback will be executed.
     *
     * @param jobId The job ID
     * @param timeoutSeconds Timeout duration in seconds
     * @param onTimeout Callback to execute when timeout fires
     */
    public void scheduleTimeout(int jobId, long timeoutSeconds, Runnable onTimeout) {
        if (timeoutSeconds <= 0) {
            log.warn("Invalid timeout {}s for job {}, skipping timeout scheduling", timeoutSeconds, jobId);
            return;
        }

        // Cancel any existing timeout for this job (shouldn't happen, but be safe)
        cancelTimeout(jobId);

        // Schedule the timeout task
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            try {
                log.warn("⏱️ Timeout fired for job {} after {}s", jobId, timeoutSeconds);
                onTimeout.run();
            } catch (Exception e) {
                log.error("Error executing timeout handler for job {}: {}", jobId, e.getMessage(), e);
            } finally {
                // Clean up the task from our map
                timeoutTasks.remove(jobId);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // Store the scheduled task so we can cancel it if job completes early
        ScheduledFuture<?> previous = timeoutTasks.put(jobId, timeoutTask);
        
        if (previous != null) {
            log.warn("Replaced existing timeout task for job {}", jobId);
        }

        log.debug("Scheduled timeout for job {} ({}s)", jobId, timeoutSeconds);
    }

    /**
     * Cancel a scheduled timeout for a job.
     * Called when a job completes before timing out.
     *
     * @param jobId The job ID
     * @return true if a timeout was canceled, false if no timeout was scheduled
     */
    public boolean cancelTimeout(int jobId) {
        ScheduledFuture<?> timeoutTask = timeoutTasks.remove(jobId);
        
        if (timeoutTask != null) {
            boolean cancelled = timeoutTask.cancel(false);  // Don't interrupt the timeout task itself
            
            if (cancelled) {
                log.debug("Cancelled timeout for job {}", jobId);
                return true;
            } else if (timeoutTask.isDone()) {
                log.debug("Timeout for job {} already fired", jobId);
            } else {
                log.warn("Failed to cancel timeout for job {}", jobId);
            }
        }
        
        return false;
    }

    /**
     * Check if a timeout is scheduled for a job.
     *
     * @param jobId The job ID
     * @return true if timeout is scheduled and not yet fired
     */
    public boolean hasTimeout(int jobId) {
        ScheduledFuture<?> task = timeoutTasks.get(jobId);
        return task != null && !task.isDone();
    }

    /**
     * Get the number of active timeout tasks.
     *
     * @return Number of scheduled timeouts
     */
    public int getActiveTimeoutCount() {
        return timeoutTasks.size();
    }

    /**
     * Gracefully shutdown the timeout scheduler.
     * Cancels all pending timeouts and waits for completion.
     */
    public void shutdown() {
        log.info("Shutting down TimeoutManager...");
        
        // Cancel all pending timeouts
        int cancelledCount = 0;
        for (Integer jobId : timeoutTasks.keySet()) {
            if (cancelTimeout(jobId)) {
                cancelledCount++;
            }
        }
        
        if (cancelledCount > 0) {
            log.info("Cancelled {} pending timeout tasks", cancelledCount);
        }

        // Shutdown the scheduler
        timeoutScheduler.shutdown();
        
        try {
            if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Timeout scheduler did not terminate in time, forcing shutdown");
                timeoutScheduler.shutdownNow();
                
                if (!timeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("Timeout scheduler did not terminate");
                }
            } else {
                log.info("✓ TimeoutManager shutdown complete");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while shutting down timeout scheduler");
            timeoutScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if the timeout scheduler is shutdown.
     *
     * @return true if shutdown
     */
    public boolean isShutdown() {
        return timeoutScheduler.isShutdown();
    }
}

