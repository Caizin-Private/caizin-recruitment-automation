package com.caizin.recruitment.service;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.entity.ParsedResume;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.caizin.recruitment.parser.ResumeParser;
import com.caizin.recruitment.repository.CandidateRepository;
import com.caizin.recruitment.util.ResumeTextExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class CandidateQuestionGenerationService {

    private final AtsPlatform atsPlatform;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeParser resumeParser;
    private final LlmService llmService;
    private final CandidateRepository candidateRepository;

    @Value("${sharepoint.download-dir}")
    private String downloadDir;

    public CandidateQuestionGenerationService(
            AtsPlatform atsPlatform,
            ResumeTextExtractor resumeTextExtractor,
            ResumeParser resumeParser,
            LlmService llmService, CandidateRepository candidateRepository
    ) {
        this.atsPlatform = atsPlatform;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeParser = resumeParser;
        this.llmService = llmService;
        this.candidateRepository = candidateRepository;
    }

    public List<ScreeningQuestionDto> generateForFirstCandidate() {

        // 1️⃣ Fetch first candidate from DB
        Candidate candidate = candidateRepository.findTopByStatus("PROCESSED")
                .orElseThrow(() -> new RuntimeException("No processed candidates found"));

        // 2️⃣ Fetch matching job from ATS
        List<JobDto> jobs = atsPlatform.fetchJobs();

        JobDto job = jobs.stream()
                .filter(j -> j.getJobOpeningId().equals(candidate.getJobOpeningId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Matching job not found in ATS"));

        // 3️⃣ Load resume file from disk
        File resumeFile = new File(downloadDir, candidate.getFileName());

        // 4️⃣ Extract text
        String resumeText = resumeTextExtractor.extractText(resumeFile);

        // 5️⃣ Parse resume
        ParsedResume parsedResume = resumeParser.parse(resumeText);

        // 6️⃣ Generate personalized questions
        return llmService.generatePersonalizedQuestions(job, parsedResume);
    }

}
