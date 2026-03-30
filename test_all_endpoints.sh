#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_URL="http://localhost:8080/api/v1/jobs"

echo "=========================================="
echo "Job Scheduler - Complete API Test Suite"
echo "=========================================="
echo ""

# Test 1: CREATE - Submit different job types
echo -e "${BLUE}Test 1: CREATE - Submit jobs of different types${NC}"
echo ""

echo "1.1 - DATA_SYNC job"
JOB1=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "MySQL",
    "targetSystem": "PostgreSQL",
    "recordCount": 5000,
    "jobPriority": "HIGH"
  }' | jq -r '.jobId')
echo -e "${GREEN}✓ Created DATA_SYNC job: $JOB1${NC}"

echo "1.2 - EMAIL_NOTIFICATION job"
JOB2=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Test Email",
    "recipientCount": 100,
    "jobPriority": "MEDIUM"
  }' | jq -r '.jobId')
echo -e "${GREEN}✓ Created EMAIL_NOTIFICATION job: $JOB2${NC}"

echo "1.3 - REPORT_GENERATION job"
JOB3=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "Monthly Sales",
    "department": "Sales",
    "jobPriority": "LOW"
  }' | jq -r '.jobId')
echo -e "${GREEN}✓ Created REPORT_GENERATION job: $JOB3${NC}"

echo "1.4 - DATA_CLEANUP job"
JOB4=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_CLEANUP",
    "tableName": "logs",
    "olderThanDays": 30,
    "jobPriority": "LOW"
  }' | jq -r '.jobId')
echo -e "${GREEN}✓ Created DATA_CLEANUP job: $JOB4${NC}"

sleep 3

echo ""
echo "=========================================="
echo ""

# Test 2: READ - Get single job
echo -e "${BLUE}Test 2: READ - Get job by ID${NC}"
echo ""

RESULT=$(curl -s $BASE_URL/$JOB1)
STATE=$(echo "$RESULT" | jq -r '.jobState')
TYPE=$(echo "$RESULT" | jq -r '.jobType')
PRIORITY=$(echo "$RESULT" | jq -r '.jobPriority')

echo "Job $JOB1:"
echo "  Type: $TYPE"
echo "  State: $STATE"
echo "  Priority: $PRIORITY"

if [ "$STATE" != "null" ]; then
    echo -e "${GREEN}✓ Successfully retrieved job${NC}"
else
    echo -e "${RED}✗ Failed to retrieve job${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 3: READ - Get all jobs
echo -e "${BLUE}Test 3: READ - Get all jobs${NC}"
echo ""

ALL_JOBS=$(curl -s $BASE_URL)
COUNT=$(echo "$ALL_JOBS" | jq 'length')

echo "Total jobs: $COUNT"
echo "$ALL_JOBS" | jq '.[] | {jobId, jobType, jobState, jobPriority}'

if [ "$COUNT" -ge 4 ]; then
    echo -e "${GREEN}✓ Retrieved all jobs${NC}"
else
    echo -e "${RED}✗ Expected at least 4 jobs, got $COUNT${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 4: Priority Queue - Submit jobs with different priorities
echo -e "${BLUE}Test 4: PRIORITY QUEUE - Test job prioritization${NC}"
echo ""

echo "Submitting 3 jobs with different priorities..."

LOW=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "Test",
    "targetSystem": "Test",
    "recordCount": 100,
    "jobPriority": "LOW"
  }' | jq -r '.jobId')

MEDIUM=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "Test",
    "targetSystem": "Test",
    "recordCount": 100,
    "jobPriority": "MEDIUM"
  }' | jq -r '.jobId')

HIGH=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "Test",
    "targetSystem": "Test",
    "recordCount": 100,
    "jobPriority": "HIGH"
  }' | jq -r '.jobId')

echo ""
echo "=========================================="
echo ""

# Test 5: CANCEL - Cancel a running job
echo -e "${BLUE}Test 5: CANCEL - Cancel a running job${NC}"
echo ""

CANCEL_JOB=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "Test",
    "targetSystem": "Test",
    "recordCount": 5000,
    "timeoutSeconds": 60
  }' | jq -r '.jobId')

echo "Created job $CANCEL_JOB with 60s timeout"
sleep 2

echo "Cancelling job $CANCEL_JOB..."
CANCEL_RESULT=$(curl -s -X DELETE $BASE_URL/$CANCEL_JOB/cancel)
echo "$CANCEL_RESULT" | jq

sleep 1

STATE=$(curl -s $BASE_URL/$CANCEL_JOB | jq -r '.jobState')
if [ "$STATE" == "CANCELLED" ]; then
    echo -e "${GREEN}✓ Job cancelled successfully${NC}"
else
    echo -e "${RED}✗ Expected CANCELLED, got: $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 6: TIMEOUT - Test job timeout
echo -e "${BLUE}Test 6: TIMEOUT - Test job timeout${NC}"
echo ""

TIMEOUT_JOB=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "Test",
    "targetSystem": "Test",
    "recordCount": 5000,
    "timeoutSeconds": 2
  }' | jq -r '.jobId')

echo "Created job $TIMEOUT_JOB with 2s timeout"

# Wait for job to start running (max 10 seconds)
echo "Waiting for job to start running..."
for i in {1..10}; do
  STATE=$(curl -s $BASE_URL/$TIMEOUT_JOB | jq -r '.jobState')
  if [ "$STATE" == "RUNNING" ] || [ "$STATE" == "TIMED_OUT" ] || [ "$STATE" == "COMPLETED" ]; then
    break
  fi
  sleep 1
done

echo "Job started, waiting for timeout..."
sleep 4

STATE=$(curl -s $BASE_URL/$TIMEOUT_JOB | jq -r '.jobState')
if [ "$STATE" == "TIMED_OUT" ]; then
    echo -e "${GREEN}✓ Job timed out correctly${NC}"
else
    echo -e "${RED}✗ Expected TIMED_OUT, got: $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 7: DELETE - Delete a job
echo -e "${BLUE}Test 7: DELETE - Delete a job from database${NC}"
echo ""

DELETE_JOB=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "To be deleted",
    "recipientCount": 1
  }' | jq -r '.jobId')

echo "Created job $DELETE_JOB"
sleep 2

echo "Verifying job exists..."
EXISTS=$(curl -s $BASE_URL/$DELETE_JOB | jq -r '.jobId')
if [ "$EXISTS" == "$DELETE_JOB" ]; then
    echo -e "${GREEN}✓ Job exists${NC}"
fi

echo "Deleting job $DELETE_JOB..."
DELETE_RESULT=$(curl -s -X DELETE $BASE_URL/$DELETE_JOB)
echo "$DELETE_RESULT" | jq

sleep 1

echo "Verifying job is deleted..."
DELETED=$(curl -s -w "%{http_code}" $BASE_URL/$DELETE_JOB -o /dev/null)
if [ "$DELETED" == "404" ]; then
    echo -e "${GREEN}✓ Job deleted successfully (404 Not Found)${NC}"
else
    echo -e "${RED}✗ Job still exists (HTTP $DELETED)${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 8: Error Handling
echo -e "${BLUE}Test 8: ERROR HANDLING${NC}"
echo ""

echo "8.1 - Get non-existent job"
RESULT=$(curl -s -w "%{http_code}" $BASE_URL/99999 -o /dev/null)
if [ "$RESULT" == "404" ]; then
    echo -e "${GREEN}✓ Returns 404 for non-existent job${NC}"
else
    echo -e "${RED}✗ Expected 404, got $RESULT${NC}"
fi

echo "8.2 - Cancel already completed job"
COMPLETED_JOB=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Quick job",
    "recipientCount": 1
  }' | jq -r '.jobId')

sleep 3

CANCEL_RESULT=$(curl -s -X DELETE $BASE_URL/$COMPLETED_JOB/cancel | jq -r '.error')
if [[ "$CANCEL_RESULT" == *"terminal state"* ]]; then
    echo -e "${GREEN}✓ Cannot cancel completed job${NC}"
else
    echo -e "${YELLOW}⚠ Unexpected response: $CANCEL_RESULT${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Summary
echo -e "${BLUE}SUMMARY${NC}"
echo ""
echo "Check MongoDB to verify persistence:"
echo "  mongosh"
echo "  use job_scheduler"
echo "  db.jobs.find().pretty()"
echo ""
echo "Check application logs for execution details"
echo ""
echo -e "${GREEN}✓ All tests completed!${NC}"
echo -e "${YELLOW}Note: HIGH priority jobs should complete first${NC}"

sleep 3

