package com.jobscheduler.util;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobPriority;
import com.jobscheduler.model.JobState;
import com.jobscheduler.model.JobType;
import com.jobscheduler.repository.JobRepositoryInterface;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final JobRepositoryInterface jobRepository;
    private final Faker faker;
    private final Random random;

    @Autowired
    public DataSeeder(JobRepositoryInterface jobRepository) {
        this.jobRepository = jobRepository;
        this.faker = new Faker();
        this.random = new Random();
    }

    /**
     * Seed jobs into the database.
     * Distribution: 50% QUEUED, 35% COMPLETED, 5% RUNNING, 10% others
     */
    public void seedJobs(int count) {
        log.info("Starting to seed {} jobs...", count);
        long startTime = System.currentTimeMillis();

        List<JobData> jobs = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            JobData job = generateRandomJob(i);
            jobs.add(job);

            // Bulk insert every 1000 jobs for performance
            if (i % 1000 == 0) {
                jobRepository.insertAll(jobs);
                jobs.clear();
                log.info("Seeded {}/{} jobs", i, count);
            }
        }

        // Insert remaining jobs
        if (!jobs.isEmpty()) {
            jobRepository.insertAll(jobs);
            log.info("Seeded {}/{} jobs", count, count);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Successfully seeded {} jobs in {}ms ({}s)",
                count, duration, duration / 1000.0);
    }

    private JobData generateRandomJob(int jobId) {
        // Randomly select job type
        JobType jobType = randomJobType();

        // Create job
        JobData job = new JobData(jobType);
        job.setJobId(jobId);

        // Set random priority
        job.setJobPriority(randomPriority());

        // Set random state with distribution: 50% QUEUED, 35% COMPLETED, 5% RUNNING, 10% others
        JobState state = randomState();
        job.setJobState(state);

        // Set random timeout (between 10s to 120s)
        job.setTimeoutSeconds((long) (10 + random.nextInt(110)));

        // Set submitted time (random time in the past 7 days)
        Instant submittedTime = Instant.now().minus(random.nextInt(7 * 24 * 60), ChronoUnit.MINUTES);
        job.setSubmittedTime(submittedTime);

        // Set start and completed times based on state
        if (state == JobState.RUNNING || state == JobState.COMPLETED ||
                state == JobState.FAILED || state == JobState.CANCELLED ||
                state == JobState.TIMED_OUT) {
            // Started 1-60 minutes after submission
            Instant startTime = submittedTime.plus(random.nextInt(60), ChronoUnit.MINUTES);
            job.setStartTime(startTime);

            // If completed/failed/cancelled/timed_out, set completion time
            if (state != JobState.RUNNING) {
                // Completed 1-30 minutes after start
                Instant completedTime = startTime.plus(random.nextInt(30), ChronoUnit.MINUTES);
                job.setCompletedTime(completedTime);
            }
        }

        // Set job-specific fields based on type
        setJobSpecificFields(job, jobType);

        return job;
    }

    private void setJobSpecificFields(JobData job, JobType jobType) {
        switch (jobType) {
            case DATA_SYNC:
                job.setSourceSystem(faker.options().option("MySQL", "PostgreSQL", "Oracle", "MongoDB", "Redis"));
                job.setTargetSystem(faker.options().option("MySQL", "PostgreSQL", "Oracle", "MongoDB", "Redis"));
                job.setRecordCount(1000 + random.nextInt(9000)); // 1000-10000
                break;

            case EMAIL_NOTIFICATION:
                job.setSubject(faker.lorem().sentence(5));
                job.setRecipientCount(1 + random.nextInt(500)); // 1-500
                break;

            case REPORT_GENERATION:
                job.setReportName(faker.options().option("Monthly Sales", "Quarterly Report",
                        "Annual Summary", "Weekly Stats",
                        "Daily Analytics"));
                job.setDepartment(faker.options().option("Sales", "Marketing", "Finance",
                        "Engineering", "HR"));
                break;

            case DATA_CLEANUP:
                job.setTableName(faker.options().option("logs", "sessions", "temp_data",
                        "cache", "old_records"));
                job.setOlderThanDays(7 + random.nextInt(90)); // 7-96 days
                break;
        }
    }

    private JobType randomJobType() {
        JobType[] types = JobType.values();
        return types[random.nextInt(types.length)];
    }

    private JobPriority randomPriority() {
        // Distribution: 20% HIGH, 50% MEDIUM, 30% LOW
        int rand = random.nextInt(100);
        if (rand < 20) return JobPriority.HIGH;
        if (rand < 70) return JobPriority.MEDIUM;
        return JobPriority.LOW;
    }

    private JobState randomState() {
        // Distribution: 50% QUEUED, 35% COMPLETED, 5% RUNNING, 10% others
        int rand = random.nextInt(100);
        if (rand < 50) return JobState.QUEUED;
        if (rand < 85) return JobState.COMPLETED;
        if (rand < 90) return JobState.RUNNING;
        if (rand < 95) return JobState.FAILED;
        if (rand < 98) return JobState.CANCELLED;
        return JobState.TIMED_OUT;
    }
}