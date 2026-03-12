#!/bin/bash

echo "Submitting 5 jobs concurrently..."
echo ""

# Submit Job 1 - ReportGeneration
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "Q1 Sales Report",
    "department": "Finance"
  }' &

# Submit Job 2 - EmailNotification
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL_NOTIFICATION",
    "subject": "Monthly Newsletter",
    "recipientCount": 100
  }' &

# Submit Job 3 - DataCleanup
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_CLEANUP",
    "tableName": "logs",
    "olderThanDays": 30
  }' &

# Submit Job 4 - DataSync
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "DATA_SYNC",
    "sourceSystem": "MySQL",
    "targetSystem": "PostgreSQL",
    "recordCount": 1000
  }' &

# Submit Job 5 - ReportGeneration
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT_GENERATION",
    "reportName": "Q2 Marketing Report",
    "department": "Marketing"
  }' &

# Wait for all background jobs to complete
wait

echo ""
echo ""
echo "All jobs submitted! Waiting for them to complete..."
echo ""

# Wait for jobs to finish (they take 3-7 seconds each)
sleep 7

# Get all jobs
echo "Fetching all jobs:"
curl http://localhost:8080/api/v1/jobs | jq '.'

