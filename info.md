Job Scheduling Engine

## Background

Your company has multiple teams that need to run background jobs — generating reports, sending notifications, cleaning up stale data, syncing records between systems. Today, each team writes their own hacky solution with `new Thread()` calls scattered across services. You need to build a **centralized job scheduling engine** that any team can use.

---

## Requirements

### 1. Job Types

The system must support **4 types of jobs**. Each has completely different logic, but the engine must be able to run any of them without knowing the internals.

| Job Type              | Inputs                                       | Behavior                                                                                          |
|-----------------------|----------------------------------------------|---------------------------------------------------------------------------------------------------|
| **Report Generation** | `reportName`, `department`                   | Heavy computation. Takes 3–5 seconds.                                                             |
| **Email Notification**| `subject`, `recipientCount`                  | Sends emails. Time is proportional to recipient count (~100ms per recipient, capped at 2 seconds).|
| **Data Cleanup**      | `tableName`, `olderThanDays`                 | Scans and deletes old records. Takes 4–7 seconds.                                                 |
| **Data Sync**         | `sourceSystem`, `targetSystem`, `recordCount`| Moves records between systems. Time proportional to record count (~2ms per record, capped at 6 seconds). |

> For now, simulate all work with `Thread.sleep()`. No real I/O.

---

### 2. Job Lifecycle

Every job moves through these states:
    SUBMITTED → QUEUED → RUNNING → COMPLETED
    └→ FAILED

The system must track:

- Job ID
- Type
- Current state
- Result message
- Submitted time
- Started time
- Completed time

---

### 3. Execution

- Jobs must execute in the background. The API must **return immediately**.
- The system must be able to run **multiple jobs concurrently** (configurable, default `5`).
- The engine must be **thread-safe** — API threads and worker threads access shared state simultaneously.

---

### 4. REST API

| Method | Endpoint           | Description                              |
|--------|--------------------|------------------------------------------|
| POST   | `/api/v1/jobs`     | Submit a new job. Returns job ID immediately. |
| GET    | `/api/v1/jobs/{id}`| Get status of a specific job.            |
| GET    | `/api/v1/jobs`     | List all jobs.                           |

---

### 5. Completion Listener

When a job finishes (success or failure), the engine must **notify registered listeners**. Listeners are pluggable — adding a new listener (audit log, alerting, metrics) must require **zero changes** to the engine code.

> For now, register a simple **logging listener** that prints job ID, type, final state, and execution time.

---

## Next Tasks

### Task A: Job Cancellation

Users must be able to cancel a job.

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/api/v1/jobs/{id}` | Cancel a queued or running job. |

**Requirements:**
- If job is **QUEUED** → mark as **CANCELLED** immediately.
- If job is **RUNNING** → interrupt it. The job must check for interruption periodically and stop gracefully.
- If job is **COMPLETED** / **FAILED** → return `409 Conflict`.
- New state: **CANCELLED**.

---

### Task B: Job Priority

Jobs must support priority: **HIGH**, **MEDIUM**, **LOW** (default **MEDIUM**).

**Requirements:**
- High priority jobs must execute before low priority jobs when multiple jobs are waiting.
- Add `priority` field to the submit API request.
- Among same priority, earlier submitted jobs go first (FIFO within priority).

---

### Task C: Job Timeout

Jobs must have a configurable timeout (default 30 seconds).

**Requirements:**
- If a job runs longer than its timeout, the system must kill it and mark it as **TIMED_OUT**.
- New state: **TIMED_OUT**.
- Add optional `timeoutSeconds` field to the submit API request.

---

### Task D: Engine Stats

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/jobs/stats` | Return engine statistics. |

**Response must include:**
- Total jobs submitted
- Count by state (queued, running, completed, failed, cancelled, timed out)
- Average execution time of completed jobs
- Currently running job count

**Note:** Multiple worker threads update these stats. The API reads them. Design for high read throughput.























