package com.jobscheduler.handler;

import com.jobscheduler.model.JobData;
import com.jobscheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationHandler implements JobTypeHandler {

    @Override
    public void execute(JobData jobData) throws InterruptedException {
        int time = Math.max(100 * jobData.getRecipientCount(), 2000);

        log.info("📧 Sending emails to {} recipients", jobData.getRecipientCount());
        log.info("   Subject: {}", jobData.getSubject());

        // Interruptible sleep - check every 100ms
        int iterations = time / 100;
        for (int i = 0; i < iterations; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Email sending cancelled");
            }
            Thread.sleep(100);
        }

        log.info("✓ Emails sent successfully");
    }
    
    @Override
    public JobType getType() {
        return JobType.EMAIL_NOTIFICATION;
    }
}

