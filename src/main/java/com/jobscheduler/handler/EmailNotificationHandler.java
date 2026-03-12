package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) {
        int time = Math.max(100 * jobData.getRecipientCount(), 2000);

        log.info("📧 Sending emails to {} recipients", jobData.getRecipientCount());
        log.info("   Subject: {}", jobData.getSubject());

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Email sending interrupted", e);
        }

        log.info("✓ Emails sent successfully");
    }
    
    @Override
    public JobType getType() {
        return JobType.EMAIL_NOTIFICATION;
    }
}

