#!/bin/bash

echo "=========================================="
echo "Priority Queue Test - Proper Scenario"
echo "Thread Pool Size: 2 workers"
echo "=========================================="
echo ""

# Step 1: Fill the thread pool with 2 long-running jobs
echo "Step 1: Filling thread pool with 2 MEDIUM priority jobs (will run for ~6 seconds each)..."

curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "MySQL",
    "targetSystem": "PostgreSQL",
    "recordCount": 1000,
    "jobPriority": "MEDIUM"
  }' | jq -r '.jobId' | xargs -I {} echo "  ✓ Submitted Job #{} (MEDIUM - DATA_SYNC)"

curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_CLEANUP",
    "tableName": "logs",
    "olderThanDays": 30,
    "jobPriority": "MEDIUM"
  }' | jq -r '.jobId' | xargs -I {} echo "  ✓ Submitted Job #{} (MEDIUM - DATA_CLEANUP)"

echo ""
echo "Step 2: Waiting 2 seconds for jobs to start running..."
sleep 2

# Step 2: Now submit jobs with different priorities - they will queue
echo ""
echo "Step 3: Submitting jobs with different priorities (these will queue)..."
echo ""

echo "  → Submitting LOW priority job..."
curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Low Priority Email",
    "recipientCount": 5,
    "jobPriority": "LOW"
  }' | jq -r '.jobId' | xargs -I {} echo "    ✓ Job #{} queued (LOW)"

sleep 0.5

echo "  → Submitting HIGH priority job..."
curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "High Priority Report",
    "department": "Finance",
    "jobPriority": "HIGH"
  }' | jq -r '.jobId' | xargs -I {} echo "    ✓ Job #{} queued (HIGH)"

sleep 0.5

echo "  → Submitting MEDIUM priority job..."
curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Medium Priority Email",
    "recipientCount": 3,
    "jobPriority": "MEDIUM"
  }' | jq -r '.jobId' | xargs -I {} echo "    ✓ Job #{} queued (MEDIUM)"

sleep 0.5

echo "  → Submitting another LOW priority job..."
curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "Low Priority Report",
    "department": "IT",
    "jobPriority": "LOW"
  }' | jq -r '.jobId' | xargs -I {} echo "    ✓ Job #{} queued (LOW)"

echo ""
echo "=========================================="
echo "Expected Execution Order (after first 2 complete):"
echo "  1. Job #4 (HIGH priority)"
echo "  2. Job #5 (MEDIUM priority)"
echo "  3. Job #3 (LOW priority - submitted first)"
echo "  4. Job #6 (LOW priority - submitted last)"
echo "=========================================="
echo ""
echo "Waiting 15 seconds for all jobs to complete..."
sleep 15

echo ""
echo "=========================================="
echo "Final Results:"
echo "=========================================="
curl -s http://localhost:8080/api/v1/jobs | jq -r '.[] | "Job #\(.jobId) - \(.jobPriority) - Started: \(.startTime)"' | sort -t'#' -k2 -n

echo ""
echo "Check server logs for detailed execution order!"

