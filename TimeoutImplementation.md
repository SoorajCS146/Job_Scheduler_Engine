# Timeout Implementation Documentation

## Overview
This document describes the production-grade timeout implementation for the Job Scheduler system, which automatically cancels jobs that exceed their configured time limits.

---

## Architecture

### Components Implemented

#### 1. **TimeoutManager** (`TimeoutManager.java`)
- **Purpose:** Centralized timeout scheduling and management
- **Technology:** `ScheduledExecutorService` with single daemon thread
- **Key Features:**
  - Non-blocking timeout scheduling
  - Thread-safe operations using `ConcurrentHashMap`
  - Automatic cleanup of fired timeouts
  - Graceful shutdown with pending timeout cancellation

#### 2. **JobScheduler Integration**
- **beforeExecute() Hook:** Schedules timeout when job starts running
- **afterExecute() Hook:** Cancels timeout when job completes successfully
- **handleTimeout() Method:** Handles timeout events by interrupting jobs and marking them as TIMED_OUT
- **shutdown() Integration:** Ensures timeout manager shuts down cleanly

#### 3. **JobExecutor Enhancement**
- **State Differentiation:** Distinguishes between TIMED_OUT and CANCELLED states
- **Proper Exception Handling:** Preserves timeout state when interrupted

#### 4. **JobData Model**
- **timeoutSeconds Field:** Configurable timeout per job (default: 30 seconds)
- **TIMED_OUT State:** New job state to indicate timeout occurred

---

## Implementation Flow

### Job Submission with Timeout
```
1. User submits job with timeoutSeconds=30
   ↓
2. JobFactory creates JobData (uses custom or default timeout)
   ↓
3. Job queued in PriorityBlockingQueue
   ↓
4. Worker thread picks up job
   ↓
5. beforeExecute() called:
   - Job state: QUEUED → RUNNING
   - Timeout scheduled (fires after 30s)
   ↓
6. Job executes
```

### Scenario A: Job Completes Before Timeout
```
7a. Job completes in 10 seconds
    ↓
8a. afterExecute() called:
    - Cancels timeout (20s remaining)
    - Removes from runningJobs
    ↓
9a. Job state: RUNNING → COMPLETED ✅
```

### Scenario B: Job Exceeds Timeout
```
7b. Job still running after 30 seconds
    ↓
8b. Timeout fires:
    - handleTimeout() called
    - Job state: RUNNING → TIMED_OUT
    - future.cancel(true) interrupts thread
    ↓
9b. Job handler receives InterruptedException
    ↓
10b. JobExecutor catch block:
     - Checks state == TIMED_OUT
     - Preserves TIMED_OUT state
     ↓
11b. Job state: TIMED_OUT ✅
```

---

## Key Design Decisions

### 1. **Single-Threaded Scheduler**
- **Choice:** `Executors.newSingleThreadScheduledExecutor()`
- **Rationale:**
  - Timeout scheduling is lightweight (just callbacks)
  - One thread can handle thousands of timeouts efficiently
  - Low memory overhead (~1MB vs 1MB per thread)
  - Uses internal `DelayQueue` for efficient scheduling

### 2. **Daemon Thread**
- **Choice:** `thread.setDaemon(true)`
- **Rationale:**
  - Allows JVM to shutdown immediately without waiting for pending timeouts
  - Timeout scheduler doesn't prevent application exit
  - Actual job threads remain non-daemon (controlled by executor shutdown)

### 3. **Schedule in beforeExecute(), Not on Submission**
- **Choice:** Schedule timeout when job starts running, not when submitted
- **Rationale:**
  - Jobs may wait in queue for extended periods
  - Timeout should only count execution time, not queue time
  - More accurate timeout enforcement

### 4. **State Differentiation (TIMED_OUT vs CANCELLED)**
- **Choice:** Separate states for timeout and manual cancellation
- **Rationale:**
  - Clear distinction for monitoring and debugging
  - Different business logic may apply (retry timeout vs respect cancellation)
  - Better analytics (timeout rate indicates performance issues)

### 5. **Race Condition Protection**
- **Choice:** Double-check job state in handleTimeout()
- **Rationale:**
  - Job might complete just as timeout fires
  - Prevents overwriting COMPLETED with TIMED_OUT
  - Defensive programming for edge cases

---

## Why This Solution is Better

### Compared to Alternative Approaches

#### ❌ **Alternative 1: Future.get(timeout)**
```java
// Blocking approach
Future<?> future = executor.submit(task);
future.get(30, TimeUnit.SECONDS);  // Blocks calling thread!
```
**Problems:**
- Blocks a thread per job (wastes resources)
- Doesn't scale to hundreds of concurrent jobs
- Requires dedicated monitoring threads

**Our Solution:**
- ✅ Non-blocking (single scheduler thread for all timeouts)
- ✅ Scales to thousands of jobs
- ✅ No dedicated monitoring threads needed

---

#### ❌ **Alternative 2: Polling Watchdog Thread**
```java
// Polling approach
while (true) {
    for (Job job : runningJobs) {
        if (job.duration() > timeout) {
            cancel(job);
        }
    }
    Thread.sleep(1000);  // Check every second
}
```
**Problems:**
- Imprecise (1-second granularity)
- Wastes CPU checking jobs that aren't close to timeout
- Complexity in handling edge cases

**Our Solution:**
- ✅ Precise (fires exactly when timeout expires)
- ✅ Event-driven (no polling overhead)
- ✅ Clean separation of concerns

---

#### ❌ **Alternative 3: Timer (Legacy Java)**
```java
// Old API
Timer timer = new Timer();
timer.schedule(new TimerTask() { ... }, delay);
```
**Problems:**
- Single-threaded (one slow callback blocks others)
- Less flexible than ScheduledExecutorService
- Doesn't integrate well with modern concurrency

**Our Solution:**
- ✅ Modern API (ScheduledExecutorService)
- ✅ Better integration with executor framework
- ✅ More flexible and maintainable

---

## Advantages of Our Implementation

### 1. **Scalability**
- Single scheduler thread handles unlimited timeouts
- O(log n) complexity for scheduling (internal heap)
- Minimal memory overhead per timeout

### 2. **Precision**
- Timeouts fire exactly when scheduled (no polling delay)
- Configurable per-job timeout (not global)
- Accurate timeout tracking

### 3. **Resource Efficiency**
- One daemon thread for all timeout management
- Automatic cleanup of completed timeouts
- No wasted CPU on polling

### 4. **Maintainability**
- Clean separation: TimeoutManager handles only timeouts
- Clear lifecycle hooks (beforeExecute/afterExecute)
- Easy to test and debug

### 5. **Production-Ready Features**
- Thread-safe operations (ConcurrentHashMap)
- Graceful shutdown with cleanup
- Proper exception handling
- Race condition protection
- Comprehensive logging

### 6. **Flexibility**
- Configurable timeout per job
- Default timeout (30s) if not specified
- Easy to extend (e.g., add timeout warnings at 80%)

---

## Configuration

### Default Timeout
```java
// In JobData.java constructor
this.timeoutSeconds = 30L;  // 30 seconds default
```

### Custom Timeout (via API)
```json
POST /api/v1/jobs
{
  "jobType": "DATA_SYNC",
  "sourceSystem": "MySQL",
  "targetSystem": "PostgreSQL",
  "recordCount": 5000,
  "timeoutSeconds": 60  // Custom 60-second timeout
}
```

### No Timeout Specified
```json
POST /api/v1/jobs
{
  "jobType": "EMAIL_NOTIFICATION",
  "subject": "Test"
  // timeoutSeconds not specified → uses default 30s
}
```

---

## Testing

### Test Coverage
1. **Job completes before timeout** → COMPLETED state
2. **Job exceeds timeout** → TIMED_OUT state
3. **Default timeout applied** → 30 seconds
4. **Custom timeout respected** → Uses specified value
5. **Timeout vs Cancellation** → Different states (TIMED_OUT vs CANCELLED)
6. **Multiple concurrent timeouts** → All handled correctly
7. **Graceful shutdown** → Pending timeouts cancelled

### Test Script
Run `test_timeout.sh` to verify all timeout scenarios.

---

## Monitoring & Observability

### Log Messages
```
// Timeout scheduled
DEBUG: Scheduled timeout for job 42 (30s)

// Timeout fired
WARN: ⏱️ Timeout fired for job 42 after 30s
WARN: ⏱️ Job 42 timed out after 30s
INFO: Job 42 marked as TIMED_OUT

// Timeout cancelled (job completed)
DEBUG: Cancelled timeout for completed job 42
```

### Metrics (Available)
- `timeoutManager.getActiveTimeoutCount()` - Number of active timeouts
- `timeoutManager.hasTimeout(jobId)` - Check if timeout scheduled
- Job state distribution (COMPLETED vs TIMED_OUT vs CANCELLED)

---

## Future Enhancements (Not Implemented)

### Potential Additions
1. **Timeout Warnings:** Alert at 80% of timeout threshold
2. **Graceful Timeout:** Allow cleanup period before hard kill
3. **Timeout Extension:** Allow jobs to request more time
4. **Per-Job-Type Defaults:** Different defaults for different job types
5. **Timeout Metrics:** Track timeout rate, average execution time
6. **Retry on Timeout:** Automatic retry with longer timeout

---

## Conclusion

This timeout implementation provides a **production-grade, scalable, and maintainable** solution for job timeout management. It leverages Java's modern concurrency APIs, follows industry best practices, and provides clear separation of concerns while being resource-efficient and precise.

**Key Takeaway:** Using `ScheduledExecutorService` with proper lifecycle integration is the industry-standard approach for timeout management, as demonstrated by frameworks like Spring Batch, Kubernetes, and Apache Airflow.

