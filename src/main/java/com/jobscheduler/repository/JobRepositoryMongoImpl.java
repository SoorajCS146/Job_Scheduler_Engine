package com.jobscheduler.repository;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobPriority;
import com.jobscheduler.model.JobState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JobRepositoryMongoImpl implements JobRepositoryInterface {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public JobRepositoryMongoImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public JobData save(JobData jobData) {
        return mongoTemplate.save(jobData);
    }

    @Override
    public Optional<JobData> findByJobId(int jobId) {
        Query query = new Query(Criteria.where("jobId").is(jobId));
        JobData result = mongoTemplate.findOne(query, JobData.class);
        return Optional.ofNullable(result);
    }

    @Override
    public List<JobData> findAll() {
        return mongoTemplate.findAll(JobData.class);
    }

    @Override
    public boolean existsByJobId(int jobId) {
        Query query = new Query(Criteria.where("jobId").is(jobId));
        return mongoTemplate.exists(query, JobData.class);
    }

    @Override
    public void deleteByJobId(int jobId) {
        Query query = new Query(Criteria.where("jobId").is(jobId));
        mongoTemplate.remove(query, JobData.class);
    }


    @Override
    public List<JobData> findQueuedJobs(int limit) {
        // Create custom sort order for priority enum
        Query query = new Query(Criteria.where("jobState").is(JobState.QUEUED))
                .with(Sort.by(
                        Sort.Order.desc("jobPriority"),  // HIGH > MEDIUM > LOW
                        Sort.Order.asc("submittedTime")   // Oldest first
                ))
                .limit(limit);

        return mongoTemplate.find(query, JobData.class);
    }

    @Override
    public void insertAll(List<JobData> jobs) {
        mongoTemplate.insertAll(jobs);
    }

    @Override
    public List<JobData> findByJobState(JobState jobState) {
        Query query = new Query(Criteria.where("jobState").is(jobState));
        return mongoTemplate.find(query, JobData.class);
    }
}