package com.caizin.recruitment.dto;

public class JobDto {
    private String jobId;
    private String title;
    private String description;
    private String experience;
    private String department;

    public JobDto() {
    }

    public JobDto(String jobId, String title, String description, String experience, String department) {
        this.jobId = jobId;
        this.title = title;
        this.description = description;
        this.experience = experience;
        this.department = department;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

