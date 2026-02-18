package com.caizin.recruitment.controller;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.dto.JobQuestionResponseDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.caizin.recruitment.service.LlmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final AtsPlatform atsPlatform;
    private final LlmService llmService;

    public DebugController(AtsPlatform atsPlatform,
                           LlmService llmService) {
        this.atsPlatform = atsPlatform;
        this.llmService = llmService;
    }

    @GetMapping("/generate")
    public JobQuestionResponseDto generateForFirstJob() {

        List<JobDto> jobs = atsPlatform.fetchJobs();

        if (jobs.isEmpty()) {
            throw new RuntimeException("No jobs found in ATS");
        }

        JobDto job = jobs.get(0);

        List<ScreeningQuestionDto> questions =
                llmService.generateQuestions(job);

        return new JobQuestionResponseDto(
                job.getJobId(),
                questions
        );
    }
}
