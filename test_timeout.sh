#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Job Timeout Test Suite"
echo "=========================================="
echo ""

# Helper function to wait for job state
wait_for_state() {
    local job_id=$1
    local expected_state=$2
    local max_attempts=30
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

# Test 1: Job completes BEFORE timeout (timeout should be cancelled)
echo -e "${BLUE}Test 1: Job completes before timeout${NC}"
echo "--------------------------------------"

echo "Submitting quick job with 30s timeout (completes in ~2s)..."
JOB1=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "EMAIL_NOTIFICATION", "subject": "Quick Test", "recipientCount": 1, "timeoutSeconds": 30}' \
  | jq -r '.jobId')
echo "  Job #$JOB1 submitted (timeout: 30s, expected duration: ~2s)"

echo "Waiting for job to complete..."
if wait_for_state $JOB1 "COMPLETED"; then
    echo -e "${GREEN}✓ Job COMPLETED${NC}"
else
    echo -e "${RED}✗ Job did not complete in time${NC}"
fi

# Check final state
RESULT=$(curl -s http://localhost:8080/api/v1/jobs/$JOB1 | jq '{jobId, jobState, jobType, timeoutSeconds}')
echo "$RESULT" | jq '.'

STATE=$(echo "$RESULT" | jq -r '.jobState')
if [ "$STATE" == "COMPLETED" ]; then
    echo -e "${GREEN}✓ Test 1 PASSED: Job completed before timeout${NC}"
else
    echo -e "${RED}✗ Test 1 FAILED: Expected COMPLETED, got $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 2: Job TIMES OUT (exceeds timeout limit)
echo -e "${BLUE}Test 2: Job times out (exceeds timeout limit)${NC}"
echo "-----------------------------------------------"

echo "Submitting long job with 3s timeout (would take ~10s)..."
JOB2=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "Test", "targetSystem": "Test", "recordCount": 5000, "timeoutSeconds": 3}' \
  | jq -r '.jobId')
echo "  Job #$JOB2 submitted (timeout: 3s, expected duration: ~10s)"

echo "Waiting for timeout to fire..."
sleep 1
echo "  t=1s: Job should be RUNNING..."
STATE_1S=$(curl -s http://localhost:8080/api/v1/jobs/$JOB2 | jq -r '.jobState')
echo "    Current state: $STATE_1S"

sleep 3
echo "  t=4s: Timeout should have fired..."
STATE_4S=$(curl -s http://localhost:8080/api/v1/jobs/$JOB2 | jq -r '.jobState')
echo "    Current state: $STATE_4S"

# Wait a bit more to ensure state is final
sleep 1

# Check final state
RESULT=$(curl -s http://localhost:8080/api/v1/jobs/$JOB2 | jq '{jobId, jobState, jobType, timeoutSeconds, startTime, completedTime}')
echo ""
echo "Final result:"
echo "$RESULT" | jq '.'

STATE=$(echo "$RESULT" | jq -r '.jobState')
if [ "$STATE" == "TIMED_OUT" ]; then
    echo -e "${GREEN}✓ Test 2 PASSED: Job correctly timed out${NC}"

    # Calculate execution time
    START=$(echo "$RESULT" | jq -r '.startTime')
    END=$(echo "$RESULT" | jq -r '.completedTime')
    if [ "$START" != "null" ] && [ "$END" != "null" ]; then
        START_SEC=$(date -j -f "%Y-%m-%dT%H:%M:%S" "${START:0:19}" +%s 2>/dev/null || echo "0")
        END_SEC=$(date -j -f "%Y-%m-%dT%H:%M:%S" "${END:0:19}" +%s 2>/dev/null || echo "0")
        if [ "$START_SEC" != "0" ] && [ "$END_SEC" != "0" ]; then
            DURATION=$((END_SEC - START_SEC))
            echo "  Execution time: ${DURATION}s (timeout was 3s)"
        fi
    fi
else
    echo -e "${RED}✗ Test 2 FAILED: Expected TIMED_OUT, got $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 3: Custom timeout vs default timeout
echo -e "${BLUE}Test 3: Custom timeout vs default timeout${NC}"
echo "------------------------------------------"

echo "Submitting job WITHOUT timeout (should use default 30s)..."
JOB3=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "EMAIL_NOTIFICATION", "subject": "Default Timeout", "recipientCount": 1}' \
  | jq -r '.jobId')

sleep 1

RESULT=$(curl -s http://localhost:8080/api/v1/jobs/$JOB3 | jq '{jobId, timeoutSeconds}')
echo "$RESULT" | jq '.'

TIMEOUT=$(echo "$RESULT" | jq -r '.timeoutSeconds')
if [ "$TIMEOUT" == "30" ]; then
    echo -e "${GREEN}✓ Test 3 PASSED: Default timeout is 30s${NC}"
else
    echo -e "${RED}✗ Test 3 FAILED: Expected 30s, got ${TIMEOUT}s${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 4: Very short timeout (1 second)
echo -e "${BLUE}Test 4: Very short timeout (1 second)${NC}"
echo "---------------------------------------"

echo "Submitting job with 1s timeout (would take ~6s)..."
JOB4=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_CLEANUP", "tableName": "test", "olderThanDays": 30, "timeoutSeconds": 1}' \
  | jq -r '.jobId')
echo "  Job #$JOB4 submitted (timeout: 1s)"

echo "Waiting for timeout..."
sleep 2

RESULT=$(curl -s http://localhost:8080/api/v1/jobs/$JOB4 | jq '{jobId, jobState, timeoutSeconds}')
echo "$RESULT" | jq '.'

STATE=$(echo "$RESULT" | jq -r '.jobState')
if [ "$STATE" == "TIMED_OUT" ]; then
    echo -e "${GREEN}✓ Test 4 PASSED: Short timeout works${NC}"
else
    echo -e "${RED}✗ Test 4 FAILED: Expected TIMED_OUT, got $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 5: Timeout vs Cancellation (ensure they're different)
echo -e "${BLUE}Test 5: Timeout vs Cancellation (different states)${NC}"
echo "---------------------------------------------------"

echo "Submitting job with 10s timeout..."
JOB5=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "Test", "targetSystem": "Test", "recordCount": 5000, "timeoutSeconds": 10}' \
  | jq -r '.jobId')
echo "  Job #$JOB5 submitted"

echo "Waiting for job to start running..."
sleep 2

echo "Manually cancelling job (before timeout)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/jobs/$JOB5)
echo "  HTTP Status: $HTTP_CODE"

sleep 1

RESULT=$(curl -s http://localhost:8080/api/v1/jobs/$JOB5 | jq '{jobId, jobState}')
echo "$RESULT" | jq '.'

STATE=$(echo "$RESULT" | jq -r '.jobState')
if [ "$STATE" == "CANCELLED" ]; then
    echo -e "${GREEN}✓ Test 5 PASSED: Manual cancellation results in CANCELLED state${NC}"
    echo "  (Not TIMED_OUT, proving they are different)"
else
    echo -e "${RED}✗ Test 5 FAILED: Expected CANCELLED, got $STATE${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Test 6: Multiple jobs with different timeouts
echo -e "${BLUE}Test 6: Multiple jobs with different timeouts${NC}"
echo "-----------------------------------------------"

echo "Submitting 3 jobs with different timeouts..."

JOB6A=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "A", "targetSystem": "B", "recordCount": 5000, "timeoutSeconds": 2}' \
  | jq -r '.jobId')
echo "  Job #$JOB6A: timeout=2s"

JOB6B=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "DATA_SYNC", "sourceSystem": "C", "targetSystem": "D", "recordCount": 5000, "timeoutSeconds": 4}' \
  | jq -r '.jobId')
echo "  Job #$JOB6B: timeout=4s"

JOB6C=$(curl -s -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{"jobType": "EMAIL_NOTIFICATION", "subject": "Test", "recipientCount": 1, "timeoutSeconds": 10}' \
  | jq -r '.jobId')
echo "  Job #$JOB6C: timeout=10s (completes quickly)"

echo ""
echo "Waiting for timeouts to fire..."
sleep 5

echo ""
echo "Checking results:"

STATE_A=$(curl -s http://localhost:8080/api/v1/jobs/$JOB6A | jq -r '.jobState')
STATE_B=$(curl -s http://localhost:8080/api/v1/jobs/$JOB6B | jq -r '.jobState')
STATE_C=$(curl -s http://localhost:8080/api/v1/jobs/$JOB6C | jq -r '.jobState')

echo "  Job #$JOB6A (2s timeout): $STATE_A"
echo "  Job #$JOB6B (4s timeout): $STATE_B"
echo "  Job #$JOB6C (10s timeout, quick job): $STATE_C"

PASS_COUNT=0
if [ "$STATE_A" == "TIMED_OUT" ]; then
    echo -e "    ${GREEN}✓ Job A timed out correctly${NC}"
    PASS_COUNT=$((PASS_COUNT + 1))
else
    echo -e "    ${RED}✗ Job A should be TIMED_OUT${NC}"
fi

if [ "$STATE_B" == "TIMED_OUT" ]; then
    echo -e "    ${GREEN}✓ Job B timed out correctly${NC}"
    PASS_COUNT=$((PASS_COUNT + 1))
else
    echo -e "    ${RED}✗ Job B should be TIMED_OUT${NC}"
fi

if [ "$STATE_C" == "COMPLETED" ]; then
    echo -e "    ${GREEN}✓ Job C completed before timeout${NC}"
    PASS_COUNT=$((PASS_COUNT + 1))
else
    echo -e "    ${RED}✗ Job C should be COMPLETED${NC}"
fi

if [ $PASS_COUNT -eq 3 ]; then
    echo -e "${GREEN}✓ Test 6 PASSED: All jobs handled correctly${NC}"
else
    echo -e "${RED}✗ Test 6 FAILED: $PASS_COUNT/3 jobs correct${NC}"
fi

echo ""
echo "=========================================="
echo ""

# Summary
echo -e "${CYAN}Test Summary${NC}"
echo "============"
echo ""
echo "Tests completed. Review results above."
echo ""
echo "Expected results:"
echo "  ✓ Test 1: Job completes before timeout → COMPLETED"
echo "  ✓ Test 2: Job exceeds timeout → TIMED_OUT"
echo "  ✓ Test 3: Default timeout is 30s"
echo "  ✓ Test 4: Short timeout (1s) works"
echo "  ✓ Test 5: Manual cancel → CANCELLED (not TIMED_OUT)"
echo "  ✓ Test 6: Multiple jobs with different timeouts"
echo ""
echo "=========================================="

