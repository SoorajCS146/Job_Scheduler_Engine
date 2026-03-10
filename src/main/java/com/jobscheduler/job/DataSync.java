package com.jobscheduler.job;

public class DataSync implements JobType {
    public String sourceSystem, targetSystem;
    public int recordCount;

    public void runJob() {
        int cappedTime = Math.max(2 * recordCount, 6000);
        System.out.println("Data sync between " + sourceSystem + " and " + targetSystem);
        try {
            Thread.sleep(cappedTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
