package com.caizin.recruitment.dto;

import java.util.List;

public class JobQuestionResponseDto {
    private String jobId;
    private List<ScreeningQuestionDto> questions;

    public JobQuestionResponseDto() {
    }

    public JobQuestionResponseDto(String jobId, List<ScreeningQuestionDto> questions) {
        this.jobId = jobId;
        this.questions = questions;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public List<ScreeningQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ScreeningQuestionDto> questions) {
        this.questions = questions;
    }
}

