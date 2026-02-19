package com.caizin.recruitment.controller;

import com.caizin.recruitment.dto.JobQuestionResponseDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.service.CandidateQuestionGenerationService;
import com.caizin.recruitment.service.SharePointService;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class CandidateQuestionController {

    private final CandidateQuestionGenerationService questionService;
    private final SharePointService sharePointService;

    public CandidateQuestionController(
            CandidateQuestionGenerationService questionService, SharePointService sharePointService
    ) {
        this.questionService = questionService;
        this.sharePointService = sharePointService;
    }

    @PostMapping("/generate")
    public JobQuestionResponseDto generateQuestions(
    ) {

        // Step 1: Fetch resumes from SharePoint
        sharePointService.processResumes();

        // Step 2: Let your service handle resume + job internally
        return questionService.generateForFirstJobAndResume();

    }
}
