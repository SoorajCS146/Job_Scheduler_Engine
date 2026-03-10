package com.jobscheduler.job;

public class DataCleanup implements JobType {
    public String tableName;
    public int olderThanDays;

    public void runJob() {
        System.out.println("Cleaning the table " + tableName + " removing records older than " + olderThanDays);
        int randomTime = (int) (Math.random() * (7 - 4 + 1)) + 4;
        try {
            Thread.sleep(randomTime * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
