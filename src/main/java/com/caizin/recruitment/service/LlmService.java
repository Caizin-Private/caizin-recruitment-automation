package com.caizin.recruitment.service;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.entity.ParsedResume;

import java.util.List;

/**
 * Abstraction for LLM-based question generation.
 * Implementations may use OpenAI or other providers.
 */
public interface LlmService {
    /**
     * Generate descriptive screening questions for the given job.
     *
     * @param job the job (title, description, experience, department)
     * @return list of screening questions (type + question text)
     */
    List<ScreeningQuestionDto> generatePersonalizedQuestions(JobDto job, ParsedResume resume);
}
