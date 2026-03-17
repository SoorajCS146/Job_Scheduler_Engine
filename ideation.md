# Job Scheduler Enhancement - REFINED Implementation Plan

## Problem Analysis

### Current Architecture
- **JobScheduler**: Uses `Executors.newFixedThreadPool(5)` with internal `LinkedBlockingQueue`
- **Issue**: ExecutorService's internal queue is FIFO - no priority support
- **Challenge**: Once a task is in ExecutorService's queue, we can't re-prioritize or cancel it

### Requirements Summary (from info.md)
1. **Job Cancellation**: Kill running jobs, remove queued jobs
2. **Job Priority**: HIGH/MEDIUM/LOW with FIFO within same priority
3. **Configurable Capacity**: Per-job timeout configuration
4. **Force Kill**: Must be able to interrupt running jobs

---

## Solution Architecture

### Core Decision: Custom ThreadPoolExecutor with PriorityBlockingQueue

**Why NOT use standard ExecutorService?**
- ❌ `Executors.newFixedThreadPool()` uses `LinkedBlockingQueue` (FIFO only)
- ❌ Cannot inject custom queue into standard executors
- ❌ No control over task prioritization or cancellation once submitted

**Why USE custom ThreadPoolExecutor?**
- ✅ Accepts custom `BlockingQueue` in constructor
- ✅ Can use `PriorityBlockingQueue` with custom comparator
- ✅ Full control over task ordering
- ✅ Supports `beforeExecute()` and `afterExecute()` hooks for tracking
- ✅ Can track and cancel individual tasks via `Future`

---

## Task A: Job Cancellation

### Research Findings
**Best Practice** (from Stack Overflow, Java Concurrency in Practice):
1. Use `Future.cancel(true)` to send interrupt signal
2. Jobs must check `Thread.currentThread().isInterrupted()` periodically
3. Graceful shutdown: catch `InterruptedException`, cleanup, exit

### Solution Design

#### 1. Track Running Jobs
```java
// In JobScheduler
private final ConcurrentHashMap<Integer, Future<?>> runningJobs = new ConcurrentHashMap<>();
```

**Why ConcurrentHashMap?**
- Thread-safe for concurrent reads/writes
- Multiple threads submit jobs, API thread cancels them
- O(1) lookup by jobId

#### 2. Store Future on Submit
```java
Future<?> future = executorService.submit(task);
runningJobs.put(jobId, future);
```

#### 3. Cancel Endpoint Logic
```java
public boolean cancelJob(int jobId) {
    JobData job = repository.findById(jobId);

    // Check state
    if (job.getJobState() == COMPLETED || job.getJobState() == FAILED) {
        throw new ConflictException("Cannot cancel completed/failed job");
    }

    if (job.getJobState() == QUEUED) {
        // Not started yet - just mark as cancelled
        job.setJobState(CANCELLED);
        // Remove from queue (handled by PriorityBlockingQueue.remove())
        return true;
    }

    if (job.getJobState() == RUNNING) {
        // Send interrupt signal
        Future<?> future = runningJobs.get(jobId);
        if (future != null) {
            future.cancel(true);  // mayInterruptIfRunning = true
        }
        job.setJobState(CANCELLED);
        return true;
    }
}
```

#### 4. Make Handlers Interruptible
**Pattern**: Check interruption in sleep loops

```java
// In ReportGenerationHandler
public void execute(JobData jobData) {
    log.info("Generating report...");

    try {
        // Simulate work in chunks, check interruption
        for (int i = 0; i < 30; i++) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Job {} interrupted, stopping gracefully", jobData.getJobId());
                throw new InterruptedException("Job cancelled");
            }
            Thread.sleep(100);  // 100ms chunks instead of 3000ms
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();  // Restore interrupt status
        throw new RuntimeException("Job cancelled by user", e);
    }
}
```

**Why check in loops?**
- `Thread.sleep()` throws `InterruptedException` when interrupted
- But if job is doing CPU work (not sleeping), need explicit checks
- Production pattern: check every iteration of long-running loops

---

## Task B: Job Priority

### Research Findings
**Production Pattern** (Quartz Scheduler, Celery, Spring Batch):
- Use `PriorityBlockingQueue` with custom `Comparator`
- Wrap tasks in a `PriorityTask` class implementing `Comparable`
- Priority levels: HIGH (1), MEDIUM (2), LOW (3) - lower number = higher priority
- Tie-breaker: submission time (FIFO within same priority)

### Solution Design

#### 1. Add Priority Enum
```java
public enum JobPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```



#### 4. Custom ThreadPoolExecutor with PriorityQueue
```java
@Component
public class JobScheduler {

    private final ThreadPoolExecutor executorService;
    private final PriorityBlockingQueue<Runnable> priorityQueue;
    private final ConcurrentHashMap<Integer, Future<?>> runningJobs;
    private final ConcurrentHashMap<Integer, PriorityTask> queuedTasks;

    public JobScheduler() {
        // Create priority queue with initial capacity
        this.priorityQueue = new PriorityBlockingQueue<>(100);

        // Track queued tasks for cancellation
        this.queuedTasks = new ConcurrentHashMap<>();

        // Track running jobs for cancellation
        this.runningJobs = new ConcurrentHashMap<>();

        // Create custom ThreadPoolExecutor
        this.executorService = new ThreadPoolExecutor(
            5,                      // corePoolSize
            5,                      // maximumPoolSize (same as core = fixed pool)
            0L,                     // keepAliveTime
            TimeUnit.MILLISECONDS,
            priorityQueue           // Our custom priority queue!
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                // Hook: called before task execution
                if (r instanceof PriorityTask) {
                    PriorityTask task = (PriorityTask) r;
                    queuedTasks.remove(task.getJobId());  // Remove from queued
                    // Job state will be set to RUNNING in JobExecutor
                }
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                // Hook: called after task execution
                if (r instanceof PriorityTask) {
                    PriorityTask task = (PriorityTask) r;
                    runningJobs.remove(task.getJobId());  // Cleanup
                }
            }
        };
    }

    public void scheduleJob(int jobId) {
        JobData jobData = jobRepository.findById(jobId);
        jobData.setJobState(JobState.QUEUED);

        // Create priority task
        PriorityTask priorityTask = new PriorityTask(jobId, jobData, () -> {
            jobExecutor.execute(jobId);
        });

        // Track in queued tasks (for cancellation)
        queuedTasks.put(jobId, priorityTask);

        // Submit to executor - goes into PriorityBlockingQueue
        Future<?> future = executorService.submit(priorityTask);

        // Track future (for cancellation)
        runningJobs.put(jobId, future);
    }
}
```

**Why this works for your constraint:**
- ✅ ALL tasks go through `PriorityBlockingQueue` - no hidden FIFO queue
- ✅ New high-priority task automatically jumps ahead in queue
- ✅ `beforeExecute()` hook tracks when task starts running
- ✅ `afterExecute()` hook cleans up completed tasks

**Key insight**: By using `ThreadPoolExecutor` constructor with custom queue, we bypass the default `LinkedBlockingQueue` entirely!

---

## Task C: Job Timeout

### Research Findings
**Best Practice** (from ExecutorService docs, production systems):
1. Use `ScheduledExecutorService` to schedule timeout checks
2. Use `Future.get(timeout, TimeUnit)` for simple cases
3. For complex cases: separate timeout monitor thread

**Problem with `Future.get(timeout)`:**
- Blocks the calling thread
- We need non-blocking timeout monitoring

### Solution Design

#### 1. Separate ScheduledExecutorService for Timeouts
```java
@Component
public class JobScheduler {

    private final ThreadPoolExecutor executorService;
    private final ScheduledExecutorService timeoutExecutor;
    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> timeoutTasks;

    public JobScheduler() {
        // Main executor for jobs
        this.executorService = new ThreadPoolExecutor(...);

        // Separate single-thread executor for timeout monitoring
        this.timeoutExecutor = Executors.newScheduledThreadPool(1);

        // Track timeout tasks
        this.timeoutTasks = new ConcurrentHashMap<>();
    }
}
```

**Why separate executor?**
- Timeout monitoring is lightweight (just checking and cancelling)
- Don't want timeout checks to compete with actual job execution
- Single thread is enough for monitoring

#### 2. Schedule Timeout on Job Start
```java
public void scheduleJob(int jobId) {
    JobData jobData = jobRepository.findById(jobId);

    // Get timeout (default 30 seconds)
    long timeoutSeconds = jobData.getTimeoutSeconds() != null
        ? jobData.getTimeoutSeconds()
        : 30;

    PriorityTask priorityTask = new PriorityTask(jobId, jobData, () -> {
        jobExecutor.execute(jobId);
    });

    queuedTasks.put(jobId, priorityTask);
    Future<?> future = executorService.submit(priorityTask);
    runningJobs.put(jobId, future);

    // Schedule timeout check
    ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() -> {
        // This runs after timeout period
        if (!future.isDone()) {
            log.warn("Job {} timed out after {}s, cancelling", jobId, timeoutSeconds);

            // Cancel the job
            future.cancel(true);

            // Update state
            JobData job = jobRepository.findById(jobId);
            job.setJobState(JobState.TIMED_OUT);
            job.setCompletedTime(Instant.now());

            // Notify listeners
            jobEventMediator.notifyListeners(job,
                new TimeoutException("Job exceeded timeout of " + timeoutSeconds + "s"));
        }
    }, timeoutSeconds, TimeUnit.SECONDS);

    // Track timeout task (for cleanup)
    timeoutTasks.put(jobId, timeoutFuture);
}
```

#### 3. Cancel Timeout on Job Completion
```java
@Override
protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    if (r instanceof PriorityTask) {
        PriorityTask task = (PriorityTask) r;
        int jobId = task.getJobId();

        // Cancel timeout task (job completed before timeout)
        ScheduledFuture<?> timeoutFuture = timeoutTasks.remove(jobId);
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);  // Don't interrupt timeout checker
        }

        runningJobs.remove(jobId);
    }
}
```

**Why this design?**
- ✅ Non-blocking: timeout check runs in separate thread
- ✅ Automatic cleanup: timeout cancelled when job completes
- ✅ Precise timing: `ScheduledExecutorService` is designed for this
- ✅ Graceful: uses same interrupt mechanism as manual cancellation

#### 4. Add Timeout Fields
```java
public class JobData {
    private Long timeoutSeconds = 30L;  // Default 30 seconds

    // Getter/setter
}

public class SubmitJobRequest {
    private Long timeoutSeconds;  // Optional

    // Getter/setter
}
```

---

## Enhanced JobState Enum

```java
public enum JobState {
    SUBMITTED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,    // New: user cancelled
    TIMED_OUT     // New: exceeded timeout
}
```

---

## Complete Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  REST API (JobController)                                   │
│  - POST /jobs (submit with priority, timeout)               │
│  - DELETE /jobs/{id} (cancel)                               │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  JobScheduler                                                │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Custom ThreadPoolExecutor                          │     │
│  │  - corePoolSize: 5                                 │     │
│  │  - queue: PriorityBlockingQueue<PriorityTask>     │     │
│  │  - beforeExecute(): track start, cancel timeout   │     │
│  │  - afterExecute(): cleanup, remove from tracking  │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │ ScheduledExecutorService (timeout monitor)        │     │
│  │  - Single thread                                   │     │
│  │  - Schedules timeout checks for each job          │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  Tracking Maps:                                             │
│  - ConcurrentHashMap<jobId, Future<?>> runningJobs         │
│  - ConcurrentHashMap<jobId, PriorityTask> queuedTasks      │
│  - ConcurrentHashMap<jobId, ScheduledFuture<?>> timeouts   │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  PriorityBlockingQueue                                      │
│  ┌──────────────────────────────────────────────────┐       │
│  │ [HIGH, t1] → [HIGH, t2] → [MEDIUM, t3] → [LOW, t4]│      │
│  │  Sorted by: 1) Priority  2) Submission time      │       │
│  └──────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Worker Threads (5 threads)                                 │
│  Thread-1: [RUNNING Job #5]                                 │
│  Thread-2: [RUNNING Job #3]                                 │
│  Thread-3: [IDLE]                                           │
│  Thread-4: [RUNNING Job #7]                                 │
│  Thread-5: [IDLE]                                           │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  JobExecutor → JobTypeHandler → JobEventMediator            │
└─────────────────────────────────────────────────────────────┘
```

---

## IMPLEMENTATION PLAN - CLEAR & ACTIONABLE

### Strategy
We'll implement in this order to achieve cancellation endpoint quickly while building the foundation for priority and timeout:

1. **Foundation First** - Add all enums, fields, and data structures
2. **Priority Queue Second** - Replace executor to enable task tracking and cancellation
3. **Cancellation Third** - Implement the DELETE endpoint (easiest win after priority queue)
4. **Timeout Last** - Add timeout monitoring (builds on cancellation mechanism)

---

### Phase 1: Foundation (Data Model Changes)
**Goal**: Add all new fields and enums needed for all features

**Files to modify**:
1. `JobState.java` - Add `CANCELLED`, `TIMED_OUT` states
2. `JobPriority.java` - Create new enum with HIGH(1), MEDIUM(2), LOW(3)
3. `JobData.java` - Add `priority` (default MEDIUM), `timeoutSeconds` (default 30L)
4. `SubmitJobRequest.java` - Add `priority`, `timeoutSeconds` fields
5. `JobResponse.java` - Add `priority`, `timeoutSeconds` to response
6. `JobFactory.java` - Update to copy new fields from request to JobData

**Why this order?**
- No dependencies - pure data model changes
- Enables all subsequent phases
- Can be done in one go

---

### Phase 2: Priority Queue & Task Tracking (Core Architecture Change)
**Goal**: Replace simple ExecutorService with custom ThreadPoolExecutor to enable priority ordering and task tracking

**Files to create**:
1. `PriorityTask.java` - Wrapper class implementing `Runnable` and `Comparable<PriorityTask>`
   - Holds jobId, jobData, actual task
   - Implements `compareTo()` for priority ordering (priority first, then submission time)

**Files to modify**:
2. `JobScheduler.java` - **MAJOR REFACTOR**
   - Replace `ExecutorService` with `ThreadPoolExecutor`
   - Create `PriorityBlockingQueue<Runnable>` with capacity 1000
   - Add tracking maps:
     - `ConcurrentHashMap<Integer, Future<?>> runningJobs` - Track running jobs for cancellation
     - `ConcurrentHashMap<Integer, PriorityTask> queuedTasks` - Track queued tasks for removal
   - Override `beforeExecute()` - Remove from queuedTasks when job starts
   - Override `afterExecute()` - Remove from runningJobs when job completes
   - Update `scheduleJob()` to wrap task in PriorityTask and track it

**Why this order?**
- This is the foundation for both cancellation and timeout
- Once we have task tracking, cancellation becomes trivial
- Priority ordering is a bonus that comes "for free" with PriorityBlockingQueue

**Testing**:
- Submit jobs with different priorities, verify HIGH runs before LOW
- Submit multiple jobs, verify FIFO within same priority

---

### Phase 3: Job Cancellation (Quick Win)
**Goal**: Implement DELETE /jobs/{id} endpoint to cancel jobs

**Files to modify**:
1. `JobScheduler.java` - Add `cancelJob(int jobId)` method
   ```java
   public boolean cancelJob(int jobId) {
       JobData job = jobRepository.findById(jobId);

       // Already finished - can't cancel
       if (job.getJobState() == COMPLETED || job.getJobState() == FAILED) {
           throw new ConflictException("Cannot cancel completed job");
       }

       // Queued - remove from queue
       if (job.getJobState() == QUEUED) {
           PriorityTask task = queuedTasks.remove(jobId);
           if (task != null) {
               priorityQueue.remove(task);  // Remove from queue
           }
           job.setJobState(CANCELLED);
           job.setCompletedTime(Instant.now());
           return true;
       }

       // Running - interrupt it
       if (job.getJobState() == RUNNING) {
           Future<?> future = runningJobs.get(jobId);
           if (future != null) {
               future.cancel(true);  // Send interrupt signal
           }
           job.setJobState(CANCELLED);
           job.setCompletedTime(Instant.now());
           return true;
       }

       return false;
   }
   ```

2. `JobService.java` - Add `cancelJob(int jobId)` method that calls scheduler

3. `JobController.java` - Add DELETE endpoint
   ```java
   @DeleteMapping("/{id}")
   public ResponseEntity<Void> cancelJob(@PathVariable int id) {
       jobService.cancelJob(id);
       return ResponseEntity.noContent().build();
   }
   ```

4. **Make handlers interruptible** - Modify all 4 handlers to check for interruption:
   - `ReportGenerationHandler.java`
   - `EmailNotificationHandler.java`
   - `DataCleanupHandler.java`
   - `DataSyncHandler.java`

   **Pattern**: Break long sleeps into smaller chunks with interruption checks
   ```java
   // Instead of: Thread.sleep(3000);
   // Do this:
   for (int i = 0; i < 30; i++) {
       if (Thread.currentThread().isInterrupted()) {
           throw new InterruptedException("Job cancelled");
       }
       Thread.sleep(100);
   }
   ```

5. `JobExecutor.java` - Update exception handling to catch `InterruptedException` and mark as CANCELLED

**Why this order?**
- Builds directly on Phase 2's task tracking
- Relatively simple - just checking maps and calling `Future.cancel()`
- Gives immediate user value (can cancel jobs)

**Testing**:
- Cancel a QUEUED job - should be removed from queue
- Cancel a RUNNING job - should interrupt and stop gracefully
- Try to cancel COMPLETED job - should return 409 Conflict

---

### Phase 4: Job Timeout (Final Feature)
**Goal**: Automatically cancel jobs that exceed their timeout

**Files to modify**:
1. `JobScheduler.java` - Add timeout monitoring
   - Add `ScheduledExecutorService timeoutExecutor` (single thread)
   - Add `ConcurrentHashMap<Integer, ScheduledFuture<?>> timeoutTasks`
   - In `scheduleJob()`: Schedule timeout check after job starts
   - In `afterExecute()`: Cancel timeout task when job completes

   ```java
   // In scheduleJob(), after submitting to executor:
   long timeout = jobData.getTimeoutSeconds() != null ? jobData.getTimeoutSeconds() : 30L;

   ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() -> {
       if (!future.isDone()) {
           log.warn("Job {} timed out after {}s", jobId, timeout);
           future.cancel(true);
           JobData job = jobRepository.findById(jobId);
           job.setJobState(TIMED_OUT);
           job.setCompletedTime(Instant.now());
           jobEventMediator.notifyListeners(job, new TimeoutException("Timeout"));
       }
   }, timeout, TimeUnit.SECONDS);

   timeoutTasks.put(jobId, timeoutFuture);
   ```

**Why this order?**
- Reuses the same interrupt mechanism as cancellation
- Timeout is just "automatic cancellation after X seconds"
- Least critical feature - can be done last

**Testing**:
- Submit job with 2s timeout, verify it gets killed
- Submit job with 60s timeout, verify it completes normally
- Verify timeout task is cancelled when job completes early

---

## Answers to Design Questions

Based on your requirements:

1. **Timeout behavior**: ✅ **Interrupt it (force kill)** - Use `Future.cancel(true)` to send interrupt signal
2. **Cancellation of queued jobs**: ✅ **Remove from queue immediately** - Mark as CANCELLED and don't execute
3. **Priority queue capacity**: ✅ **Configurable capacity** - Use bounded `PriorityBlockingQueue` with configurable size (default 1000)
4. **Timeout granularity**: ✅ **Per-job configurable** - Each job can specify its own timeout (default 30s)
5. **Failed cancellation**: ✅ **Force kill** - Send interrupt, mark as CANCELLED, let thread cleanup happen naturally

---

## COMPLETE FILE CHECKLIST

### Phase 1: Foundation
- [ ] `src/main/java/com/jobscheduler/model/JobState.java` - Add CANCELLED, TIMED_OUT
- [ ] `src/main/java/com/jobscheduler/model/JobPriority.java` - **NEW FILE** - Create enum
- [ ] `src/main/java/com/jobscheduler/model/JobData.java` - Add priority, timeoutSeconds fields
- [ ] `src/main/java/com/jobscheduler/dto/SubmitJobRequest.java` - Add priority, timeoutSeconds fields
- [ ] `src/main/java/com/jobscheduler/dto/JobResponse.java` - Add priority, timeoutSeconds fields
- [ ] `src/main/java/com/jobscheduler/factory/JobFactory.java` - Copy new fields from request to JobData

### Phase 2: Priority Queue
- [ ] `src/main/java/com/jobscheduler/engine/PriorityTask.java` - **NEW FILE** - Create wrapper class
- [ ] `src/main/java/com/jobscheduler/engine/JobScheduler.java` - **MAJOR REFACTOR** - Replace executor, add tracking

### Phase 3: Cancellation
- [ ] `src/main/java/com/jobscheduler/engine/JobScheduler.java` - Add cancelJob() method
- [ ] `src/main/java/com/jobscheduler/service/JobService.java` - Add cancelJob() method
- [ ] `src/main/java/com/jobscheduler/controller/JobController.java` - Add DELETE endpoint
- [ ] `src/main/java/com/jobscheduler/handler/ReportGenerationHandler.java` - Make interruptible
- [ ] `src/main/java/com/jobscheduler/handler/EmailNotificationHandler.java` - Make interruptible
- [ ] `src/main/java/com/jobscheduler/handler/DataCleanupHandler.java` - Make interruptible
- [ ] `src/main/java/com/jobscheduler/handler/DataSyncHandler.java` - Make interruptible
- [ ] `src/main/java/com/jobscheduler/engine/JobExecutor.java` - Handle InterruptedException
- [ ] `src/main/java/com/jobscheduler/exception/ConflictException.java` - **NEW FILE** - For 409 errors

### Phase 4: Timeout
- [ ] `src/main/java/com/jobscheduler/engine/JobScheduler.java` - Add timeout executor and monitoring

**Total**: 1 new enum, 2 new classes, 1 new exception, 11 modified files

---

---

## SUMMARY: What We're Building

### New Components
1. **JobPriority enum** - HIGH(1), MEDIUM(2), LOW(3)
2. **PriorityTask class** - Wrapper for tasks with priority comparison logic
3. **Custom ThreadPoolExecutor** - Replaces simple ExecutorService
4. **Task tracking maps** - Track queued and running jobs for cancellation
5. **Timeout executor** - Separate thread pool for timeout monitoring

### Modified Components
1. **JobState** - Add CANCELLED, TIMED_OUT states
2. **JobData** - Add priority, timeoutSeconds fields
3. **JobScheduler** - Complete refactor to use custom executor
4. **All handlers** - Make interruptible with periodic checks
5. **JobExecutor** - Handle InterruptedException
6. **DTOs** - Add new fields to request/response

### New Endpoints
1. `DELETE /api/v1/jobs/{id}` - Cancel a job

### Key Technical Decisions
- **Queue capacity**: 1000 (configurable)
- **Default timeout**: 30 seconds (per-job configurable)
- **Cancellation strategy**: Interrupt signal via `Future.cancel(true)`
- **Timeout strategy**: Scheduled check + interrupt (reuses cancellation)
- **Priority ordering**: Priority first, submission time second (FIFO within priority)

---


