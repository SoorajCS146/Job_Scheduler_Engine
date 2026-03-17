#!/bin/bash

echo "Testing Priority Queue - Submitting jobs with different priorities..."
echo ""

# Submit Job 1 - LOW Priority
echo "Job 1: LOW priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "Low Priority Report",
    "department": "IT",
    "jobPriority": "LOW"
  }' &

# Submit Job 2 - LOW Priority
echo "Job 2: LOW priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Low Priority Email",
    "recipientCount": 5,
    "jobPriority": "LOW"
  }' &

# Submit Job 3 - HIGH Priority
echo "Job 3: HIGH priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_CLEANUP",
    "tableName": "logs",
    "olderThanDays": 30,
    "jobPriority": "HIGH"
  }' &

# Submit Job 4 - MEDIUM Priority
echo "Job 4: MEDIUM priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "MySQL",
    "targetSystem": "PostgreSQL",
    "recordCount": 1000,
    "jobPriority": "MEDIUM"
  }' &

# Submit Job 5 - HIGH Priority
echo "Job 5: HIGH priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "High Priority Report",
    "department": "Finance",
    "jobPriority": "HIGH"
  }' &

# Submit Job 6 - MEDIUM Priority
echo "Job 6: MEDIUM priority"
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Medium Priority Email",
    "recipientCount": 3,
    "jobPriority": "MEDIUM"
  }' &

# Wait for all background jobs to complete
wait

echo ""
echo "=========================================="
echo "All jobs submitted!"
echo "Expected execution order: HIGH → HIGH → MEDIUM → MEDIUM → LOW → LOW"
echo "Check server logs to verify priority ordering"
echo "=========================================="
echo ""

# Wait for jobs to finish
sleep 8

# Get all jobs
echo "Fetching all jobs:"
curl -s http://localhost:8080/api/v1/jobs | jq '.'

