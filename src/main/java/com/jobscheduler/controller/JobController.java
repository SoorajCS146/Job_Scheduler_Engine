package com.jobscheduler.controller;

import com.jobscheduler.dto.JobResponse;
import com.jobscheduler.dto.SubmitJobRequest;
import com.jobscheduler.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for job operations.
 * Uses DTOs and delegates to service layer.
 * NO SWITCH STATEMENT - clean and focused on HTTP concerns.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    /**
     * Submit a new job.
     *
     * @param request SubmitJobRequest DTO
     * @return Job ID
     */
    @PostMapping
    public ResponseEntity<Map<String, Integer>> submitJob(@RequestBody SubmitJobRequest request) {
        int jobId = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("jobId", jobId));
    }

    /**
     * Get a job by ID.
     *
     * @param id Job ID
     * @return JobResponse DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable int id) {
        JobResponse response = jobService.getJob(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get all jobs.
     *
     * @return List of JobResponse DTOs
     */
    @GetMapping
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        List<JobResponse> jobs = jobService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelJob(@PathVariable int id) {
        jobService.cancelJob(id);
        return ResponseEntity.noContent().build();
    }
}