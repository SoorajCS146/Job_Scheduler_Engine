package com.jobscheduler.service;

import com.jobscheduler.util.DataSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);
    private static final int MAX_SEED_COUNT = 1_000_000;

    private final DataSeeder dataSeeder;

    @Autowired
    public DataSeederService(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    /**
     * Seed jobs with validation.
     *
     * @param count Number of jobs to seed
     * @return Duration in milliseconds
     * @throws IllegalArgumentException if count is invalid
     */
    public long seedJobs(int count) {
        validateCount(count);

        log.info("Service: Starting to seed {} jobs", count);
        long startTime = System.currentTimeMillis();

        dataSeeder.seedJobs(count);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Service: Completed seeding {} jobs in {}ms", count, duration);

        return duration;
    }

    private void validateCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than 0");
        }
        if (count > MAX_SEED_COUNT) {
            throw new IllegalArgumentException("Count must not exceed " + MAX_SEED_COUNT);
        }
    }
}