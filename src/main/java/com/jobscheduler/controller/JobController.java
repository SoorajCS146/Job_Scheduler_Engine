package com.jobscheduler.controller;

import com.jobscheduler.engine.JobExecutor;
import com.jobscheduler.engine.JobScheduler;
import com.jobscheduler.job.*;
import com.jobscheduler.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobExecutor jobExecutor;

    @Autowired
    private JobScheduler jobScheduler;

    @PostMapping
    public ResponseEntity<Map<String, Integer>> submitJob(@RequestBody Map<String, Object> request) {
        String jobType = (String) request.get("jobType");

        JobType job = createJobFromRequest(jobType, request);

        Job jobWrapper = new Job(job);
        int jobId = jobExecutor.submitJob(jobWrapper);
        jobScheduler.scheduleJob(jobId);

        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    private JobType createJobFromRequest(String jobType, Map<String, Object> request) {
        switch (jobType) {
            case "ReportGeneration":
                ReportGeneration rg = new ReportGeneration();
                rg.reportName = (String) request.get("reportName");
                rg.department = (String) request.get("department");
                return rg;

            case "EmailNotification":
                EmailNotification en = new EmailNotification();
                en.subject = (String) request.get("subject");
                en.recipientCount = ((Number) request.get("recipientCount")).intValue();
                return en;

            case "DataCleanup":
                DataCleanup dc = new DataCleanup();
                dc.tableName = (String) request.get("tableName");
                dc.olderThanDays = ((Number) request.get("olderThanDays")).intValue();
                return dc;

            case "DataSync":
                DataSync ds = new DataSync();
                ds.sourceSystem = (String) request.get("sourceSystem");
                ds.targetSystem = (String) request.get("targetSystem");
                ds.recordCount = ((Number) request.get("recordCount")).intValue();
                return ds;

            default:
                throw new IllegalArgumentException("Unknown job type: " + jobType);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable int id) {
        return ResponseEntity.ok(jobExecutor.getJobById(id));
    }

    @GetMapping
    public ResponseEntity<Map<Integer, Job>> getAllJobs() {
        // Get all jobs
        return ResponseEntity.ok(jobExecutor.getAllJobs());
        // Return map
    }
}