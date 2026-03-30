package com.jobscheduler.controller;

import com.jobscheduler.service.DataSeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/seed")
public class DataSeederController {

    @Autowired
    private DataSeederService dataSeederService;

    /**
     * Seed jobs into the database.
     *
     * Example: POST /api/v1/seed/100000
     *
     * @param count Number of jobs to seed
     * @return Response with seeding stats
     */
    @PostMapping("/{count}")
    public ResponseEntity<Map<String, Object>> seedJobs(@PathVariable int count) {
        try {
            long durationMs = dataSeederService.seedJobs(count);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully seeded jobs",
                    "count", count,
                    "durationMs", durationMs,
                    "durationSeconds", durationMs / 1000.0
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}