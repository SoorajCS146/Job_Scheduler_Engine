package com.jobscheduler.job;

public class ReportGeneration implements JobType {
    public String reportName;
    public String department;

    public void runJob() {
        System.out.println("Generating Report, Be patient");
        try {
            Thread.sleep(3000 + (int) (Math.random() * (5 - 3 + 1)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Report " + reportName + " is generated from the " + department + " department");
    }
}