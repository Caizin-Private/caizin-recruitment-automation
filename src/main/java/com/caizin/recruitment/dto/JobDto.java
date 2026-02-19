package com.caizin.recruitment.dto;

public class JobDto {

    private String zohoJobid;                // Zoho internal ID
    private String jobOpeningId;      // ZR_1_JOB (custom job code)
    private String title;
    private String description;
    private String experience;
    private String department;

    public JobDto() {}

    public JobDto(
            String zohoJobid,
            String jobOpeningId,
            String title,
            String description,
            String experience,
            String department
    ) {
        this.zohoJobid = zohoJobid;
        this.jobOpeningId = jobOpeningId;
        this.title = title;
        this.description = description;
        this.experience = experience;
        this.department = department;
    }

    // 🔹 Internal Zoho ID

    public String getZohoJobid() {
        return zohoJobid;
    }

    public void setZohoJobid(String zohoJobid) {
        this.zohoJobid = zohoJobid;
    }

    // 🔹 Custom Job Opening ID (ZR_1_JOB)
    public String getJobOpeningId() {
        return jobOpeningId;
    }

    public void setJobOpeningId(String jobOpeningId) {
        this.jobOpeningId = jobOpeningId;
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
