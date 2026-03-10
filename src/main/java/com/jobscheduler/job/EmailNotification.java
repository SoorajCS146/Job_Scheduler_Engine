package com.jobscheduler.job;

public class EmailNotification implements JobType {
    public String subject;
    public int recipientCount;

    public void runJob() {
        int cappedTimed = Math.max(100 * recipientCount, 2000);
        System.out.println("Email Notification are being sent to " + recipientCount + " of subject " + subject);
        try {
            Thread.sleep(cappedTimed);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
