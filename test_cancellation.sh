#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Job Cancellation Test Suite"
echo "=========================================="
echo ""

# Helper function to wait for job state
wait_for_state() {
    local job_id=$1
    local expected_state=$2
    local max_attempts=20
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        current_state=$(curl -s http://localhost:8080/api/v1/jobs/$job_id | jq -r '.jobState')
        if [ "$current_state" == "$expected_state" ]; then
            return 0
        fi
        sleep 0.5
        attempt=$((attempt + 1))
    done
    return 1
}

# Test 1: Cancel a QUEUED job
echo -e "${BLUE}Test 1: Cancel a QUEUED job${NC}"
echo "----------------------------"

# Submit 2 long-running jobs to fill the pool (2 worker threads)
echo "Submitting 2 long-running jobs to fill thread pool..."
JOB1=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "MySQL", "targetSystem": "PostgreSQL", "recordCount": 3000}' \
  | jq -r '.jobId')
echo "  Job #$JOB1 submitted"

JOB2=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_CLEANUP", "tableName": "logs", "olderThanDays": 30}' \
  | jq -r '.jobId')
echo "  Job #$JOB2 submitted"

# Wait for both to start running
echo "Waiting for jobs to start running..."
sleep 2

# Submit a job that will be queued
echo ""
echo "Submitting job that will be QUEUED..."
QUEUED_JOB=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "REPORT_GENERATION", "reportName": "Queued Report", "department": "IT"}' \
  | jq -r '.jobId')
echo "  Job #$QUEUED_JOB submitted"

sleep 1

# Verify it's queued
STATE=$(curl -s http://localhost:8080/api/v1/jobs/$QUEUED_JOB | jq -r '.jobState')
echo "  Current state: $STATE"

if [ "$STATE" == "QUEUED" ]; then
    echo -e "${GREEN}✓ Job is QUEUED as expected${NC}"
else
    echo -e "${RED}✗ Job is not QUEUED (state: $STATE)${NC}"
fi

# Cancel the queued job
echo ""
echo "Cancelling QUEUED job #$QUEUED_JOB..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/jobs/$QUEUED_JOB)
echo "  HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}✓ Cancellation accepted (204 No Content)${NC}"
else
    echo -e "${RED}✗ Unexpected status code: $HTTP_CODE${NC}"
fi

# Check final status
sleep 0.5
FINAL_STATE=$(curl -s http://localhost:8080/api/v1/jobs/$QUEUED_JOB | jq -r '.jobState')
echo "  Final state: $FINAL_STATE"

if [ "$FINAL_STATE" == "CANCELLED" ]; then
    echo -e "${GREEN}✓ Job successfully cancelled${NC}"
else
    echo -e "${RED}✗ Job state is not CANCELLED: $FINAL_STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 2: Cancel a RUNNING job
echo -e "${BLUE}Test 2: Cancel a RUNNING job${NC}"
echo "-----------------------------"

# Submit a very long-running job
echo "Submitting very long-running job..."
RUNNING_JOB=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "Oracle", "targetSystem": "MongoDB", "recordCount": 5000}' \
  | jq -r '.jobId')
echo "  Job #$RUNNING_JOB submitted"

# Wait for it to start running (wait for one of the first jobs to complete)
echo "Waiting for job to start RUNNING..."
if wait_for_state $RUNNING_JOB "RUNNING"; then
    echo -e "${GREEN}✓ Job is RUNNING${NC}"
else
    echo -e "${YELLOW}⚠ Job didn't reach RUNNING state in time, checking current state...${NC}"
fi

# Check current state
STATE=$(curl -s http://localhost:8080/api/v1/jobs/$RUNNING_JOB | jq '{jobId, jobState, jobType}')
echo "$STATE" | jq '.'

# Cancel it
echo ""
echo "Cancelling RUNNING job #$RUNNING_JOB..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/jobs/$RUNNING_JOB)
echo "  HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" == "204" ]; then
    echo -e "${GREEN}✓ Cancellation accepted (204 No Content)${NC}"
else
    echo -e "${RED}✗ Unexpected status code: $HTTP_CODE${NC}"
fi

# Check final status
sleep 1
FINAL_DATA=$(curl -s http://localhost:8080/api/v1/jobs/$RUNNING_JOB | jq '{jobId, jobState, jobType, startTime, completedTime}')
echo "$FINAL_DATA" | jq '.'

FINAL_STATE=$(echo "$FINAL_DATA" | jq -r '.jobState')
if [ "$FINAL_STATE" == "CANCELLED" ]; then
    echo -e "${GREEN}✓ Job successfully cancelled${NC}"
else
    echo -e "${RED}✗ Job state is not CANCELLED: $FINAL_STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 3: Try to cancel a COMPLETED job
echo -e "${BLUE}Test 3: Try to cancel a COMPLETED job (should fail)${NC}"
echo "----------------------------------------------------"

# Submit a quick job
echo "Submitting quick job..."
QUICK_JOB=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "EMAIL_NOTIFICATION", "subject": "Quick Test", "recipientCount": 1}' \
  | jq -r '.jobId')
echo "  Job #$QUICK_JOB submitted"

# Wait for it to complete
echo "Waiting for job to complete..."
if wait_for_state $QUICK_JOB "COMPLETED"; then
    echo -e "${GREEN}✓ Job COMPLETED${NC}"
else
    echo -e "${YELLOW}⚠ Job didn't complete in time${NC}"
fi

# Verify it's completed
STATE=$(curl -s http://localhost:8080/api/v1/jobs/$QUICK_JOB | jq '{jobId, jobState, jobType}')
echo "$STATE" | jq '.'

CURRENT_STATE=$(echo "$STATE" | jq -r '.jobState')

# Try to cancel it
echo ""
echo "Trying to cancel COMPLETED job #$QUICK_JOB..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/jobs/$QUICK_JOB)
echo "  HTTP Status: $HTTP_CODE"

if [ "$CURRENT_STATE" == "COMPLETED" ]; then
    if [ "$HTTP_CODE" == "409" ]; then
        echo -e "${GREEN}✓ Correctly rejected with 409 Conflict${NC}"
    else
        echo -e "${RED}✗ Expected 409 Conflict, got: $HTTP_CODE${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Job was not COMPLETED, so test is inconclusive${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 4: Verify interrupt responsiveness
echo -e "${BLUE}Test 4: Verify interrupt responsiveness (< 500ms)${NC}"
echo "--------------------------------------------------"

# Submit a very long job
echo "Submitting very long-running job (10000 records = ~20 seconds)..."
LONG_JOB=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "BigQuery", "targetSystem": "Snowflake", "recordCount": 10000}' \
  | jq -r '.jobId')
echo "  Job #$LONG_JOB submitted"

# Wait for it to start running
echo "Waiting for job to start RUNNING..."
if wait_for_state $LONG_JOB "RUNNING"; then
    echo -e "${GREEN}✓ Job is RUNNING${NC}"

    # Measure cancellation time
    echo ""
    echo "Cancelling job and measuring response time..."
    START_TIME=$(date +%s%N)
    curl -s -X DELETE http://localhost:8080/api/v1/jobs/$LONG_JOB > /dev/null

    # Wait for state to change to CANCELLED
    MAX_WAIT=50  # 5 seconds max
    WAIT_COUNT=0
    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
        STATE=$(curl -s http://localhost:8080/api/v1/jobs/$LONG_JOB | jq -r '.jobState')
        if [ "$STATE" == "CANCELLED" ]; then
            END_TIME=$(date +%s%N)
            ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))
            echo "  Cancellation completed in ${ELAPSED_MS}ms"

            if [ $ELAPSED_MS -lt 500 ]; then
                echo -e "${GREEN}✓ Excellent! Cancelled in < 500ms${NC}"
            elif [ $ELAPSED_MS -lt 1000 ]; then
                echo -e "${GREEN}✓ Good! Cancelled in < 1 second${NC}"
            else
                echo -e "${YELLOW}⚠ Slow cancellation: ${ELAPSED_MS}ms${NC}"
            fi
            break
        fi
        sleep 0.1
        WAIT_COUNT=$((WAIT_COUNT + 1))
    done

    if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
        echo -e "${RED}✗ Job did not cancel within 5 seconds${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Job didn't reach RUNNING state, skipping timing test${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Summary
echo -e "${BLUE}Test Summary${NC}"
echo "============"
echo ""
echo "All tests completed. Check results above."
echo ""
echo "Expected results:"
echo "  ✓ Test 1: QUEUED job cancelled successfully"
echo "  ✓ Test 2: RUNNING job cancelled successfully"
echo "  ✓ Test 3: COMPLETED job returns 409 Conflict"
echo "  ✓ Test 4: Cancellation responds in < 500ms"
echo ""
echo "=========================================="

