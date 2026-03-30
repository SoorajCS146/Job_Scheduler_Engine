package com.jobscheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);
    
    private final PerformanceBenchmark performanceBenchmark;

    @Autowired
    public BenchmarkService(PerformanceBenchmark performanceBenchmark) {
        this.performanceBenchmark = performanceBenchmark;
    }

    /**
     * Run dispatcher query benchmark.
     */
    public PerformanceBenchmark.BenchmarkResult runDispatcherBenchmark(int iterations) {
        validateIterations(iterations);
        log.info("Service: Running dispatcher benchmark with {} iterations", iterations);
        return performanceBenchmark.benchmarkDispatcherQuery(iterations);
    }

    /**
     * Run findByJobId benchmark.
     */
    public PerformanceBenchmark.BenchmarkResult runFindByJobIdBenchmark(int iterations) {
        validateIterations(iterations);
        log.info("Service: Running findByJobId benchmark with {} iterations", iterations);
        return performanceBenchmark.benchmarkFindByJobId(iterations);
    }

    private void validateIterations(int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be greater than 0");
        }
        if (iterations > 10000) {
            throw new IllegalArgumentException("Iterations must not exceed 10000");
        }
    }
}

