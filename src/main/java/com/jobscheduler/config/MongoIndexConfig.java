package com.jobscheduler.config;

import com.jobscheduler.model.JobData;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Creating MongoDB indexes...");

        IndexOperations indexOps = mongoTemplate.indexOps(JobData.class);

        // Index 1: jobId (unique) - for fast lookups by business ID
        Index jobIdIndex = new Index()
                .on("jobId", Sort.Direction.ASC)
                .unique()
                .named("idx_jobId");

        indexOps.ensureIndex(jobIdIndex);
        log.info("✓ Created index: idx_jobId");

        // Index 2: Compound index for dispatcher query (CRITICAL!)
        // Query: { jobState: "QUEUED" } sort by { jobPriority: -1, submittedTime: 1 }
        Index dispatcherIndex = new Index()
                .on("jobState", Sort.Direction.ASC)
                .on("jobPriority", Sort.Direction.DESC)
                .on("submittedTime", Sort.Direction.ASC)
                .named("idx_dispatcher");

        indexOps.ensureIndex(dispatcherIndex);
        log.info("✓ Created index: idx_dispatcher (compound: jobState + jobPriority + submittedTime)");

        log.info("✅ All indexes created successfully!");
    }
}