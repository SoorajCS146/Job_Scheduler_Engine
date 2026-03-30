package com.jobscheduler.controller;

import com.jobscheduler.service.BenchmarkService;
import com.jobscheduler.service.PerformanceBenchmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/benchmark")
public class BenchmarkController {

    @Autowired
    private BenchmarkService benchmarkService;

    /**
     * Benchmark dispatcher query performance.
     * 
     * Example: POST /api/v1/benchmark/dispatcher/100
     * 
     * @param iterations Number of times to run the query
     * @return Benchmark results
     */
    @PostMapping("/dispatcher/{iterations}")
    public ResponseEntity<Map<String, Object>> benchmarkDispatcherQuery(@PathVariable int iterations) {
        try {
            PerformanceBenchmark.BenchmarkResult result = benchmarkService.runDispatcherBenchmark(iterations);
            
            return ResponseEntity.ok(Map.of(
                    "query", "Dispatcher Query (findQueuedJobs)",
                    "iterations", result.iterations,
                    "avgMs", result.avgMs,
                    "minMs", result.minMs,
                    "maxMs", result.maxMs,
                    "totalMs", result.totalMs
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Benchmark findByJobId query performance.
     * 
     * Example: POST /api/v1/benchmark/findByJobId/100
     * 
     * @param iterations Number of times to run the query
     * @return Benchmark results
     */
    @PostMapping("/findByJobId/{iterations}")
    public ResponseEntity<Map<String, Object>> benchmarkFindByJobId(@PathVariable int iterations) {
        try {
            PerformanceBenchmark.BenchmarkResult result = benchmarkService.runFindByJobIdBenchmark(iterations);
            
            return ResponseEntity.ok(Map.of(
                    "query", "Find By JobId",
                    "iterations", result.iterations,
                    "avgMs", result.avgMs,
                    "minMs", result.minMs,
                    "maxMs", result.maxMs,
                    "totalMs", result.totalMs
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

