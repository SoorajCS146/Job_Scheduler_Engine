package com.jobscheduler.service;

import com.jobscheduler.model.JobData;
import com.jobscheduler.repository.JobRepositoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerformanceBenchmark {

    private static final Logger log = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    private final JobRepositoryInterface jobRepository;

    @Autowired
    public PerformanceBenchmark(JobRepositoryInterface jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Benchmark the dispatcher query (most critical query).
     * Runs the query multiple times and calculates average time.
     */
    public BenchmarkResult benchmarkDispatcherQuery(int iterations) {
        log.info("Starting benchmark: {} iterations", iterations);
        
        // Warm up (don't count first run)
        jobRepository.findQueuedJobs(5);
        
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            List<JobData> jobs = jobRepository.findQueuedJobs(5);
            
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            
            totalTime += duration;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
            
            if ((i + 1) % 10 == 0) {
                log.info("Completed {}/{} iterations", i + 1, iterations);
            }
        }
        
        double avgTime = totalTime / (double) iterations;
        
        BenchmarkResult result = new BenchmarkResult(
            iterations,
            avgTime,
            minTime,
            maxTime,
            totalTime
        );
        
        log.info("Benchmark complete: avg={}ms, min={}ms, max={}ms", 
                 avgTime, minTime, maxTime);
        
        return result;
    }

    /**
     * Benchmark find by jobId query.
     */
    public BenchmarkResult benchmarkFindByJobId(int iterations) {
        log.info("Starting findByJobId benchmark: {} iterations", iterations);
        
        // Get a random job ID that exists
        List<JobData> allJobs = jobRepository.findAll();
        if (allJobs.isEmpty()) {
            throw new IllegalStateException("No jobs found in database");
        }
        int testJobId = allJobs.get(0).getJobId();
        
        // Warm up
        jobRepository.findByJobId(testJobId);
        
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            jobRepository.findByJobId(testJobId);
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            
            totalTime += duration;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
        }
        
        double avgTime = totalTime / (double) iterations;
        
        BenchmarkResult result = new BenchmarkResult(
            iterations,
            avgTime,
            minTime,
            maxTime,
            totalTime
        );
        
        log.info("FindByJobId benchmark complete: avg={}ms, min={}ms, max={}ms", 
                 avgTime, minTime, maxTime);
        
        return result;
    }

    public static class BenchmarkResult {
        public final int iterations;
        public final double avgMs;
        public final long minMs;
        public final long maxMs;
        public final long totalMs;

        public BenchmarkResult(int iterations, double avgMs, long minMs, long maxMs, long totalMs) {
            this.iterations = iterations;
            this.avgMs = avgMs;
            this.minMs = minMs;
            this.maxMs = maxMs;
            this.totalMs = totalMs;
        }
    }
}

