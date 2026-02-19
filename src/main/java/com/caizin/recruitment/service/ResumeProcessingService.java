package com.caizin.recruitment.service;

import com.caizin.recruitment.entity.JDRequirements;
import com.caizin.recruitment.entity.ParsedResume;
import com.caizin.recruitment.integration.MCPClient.MCPClient;
import com.caizin.recruitment.parser.JDParser;
import com.caizin.recruitment.parser.ResumeParser;
import com.caizin.recruitment.util.JDTextExtractor;
import com.caizin.recruitment.util.ResumeTextExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Service
public class ResumeProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeProcessingService.class);

    private final ResumeTextExtractor extractor;
    private final ResumeParser resumeParser;
    private final JDParser jdParser;
    private final JDTextExtractor jdExtractor;
    private final ATSScoringService atsScoringService;
    private final MCPClient mcpClient;
    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeProcessingService(
            ResumeTextExtractor extractor,
            ResumeParser resumeParser,
            JDParser jdParser,
            JDTextExtractor jdExtractor,
            ATSScoringService atsScoringService,
            MCPClient mcpClient,
            ResumeAnalysisService resumeAnalysisService
    ) {

        this.extractor = extractor;
        this.resumeParser = resumeParser;
        this.jdParser = jdParser;
        this.jdExtractor = jdExtractor;
        this.atsScoringService = atsScoringService;
        this.mcpClient = mcpClient;
        this.resumeAnalysisService = resumeAnalysisService;
    }

    public double process(
            File file,
            String senderName,
            String senderEmail,
            String jobOpeningId,
            String candidateId
    ) {

        try {

            log.info("Processing resume for jobId={}, candidateId={}",
                    jobOpeningId, candidateId);

            String resumeText =
                    extractor.extractText(file);

            ParsedResume parsedResume =
                    resumeParser.parse(resumeText);

            String email =
                    parsedResume.email() == null ||
                            parsedResume.email().isBlank() ||
                            parsedResume.email().equals("unknown@email.com")
                            ? senderEmail
                            : parsedResume.email();

            String fullName =
                    parsedResume.fullName() == null ||
                            parsedResume.fullName().isBlank() ||
                            parsedResume.fullName().equals("UNKNOWN")
                            ? senderName
                            : parsedResume.fullName();

            String jdText =
                    jdExtractor.getJDText(jobOpeningId);

            JDRequirements jdRequirements =
                    jdParser.parse(jdText);

            double atsScore =
                    atsScoringService.calculate(
                            resumeText,
                            jdText,
                            parsedResume,
                            jdRequirements
                    );

            Map<String, Object> aiAnalysis =
                    mcpClient.analyzeResume(
                            resumeText,
                            jdText,
                            jobOpeningId,
                            candidateId
                    );

            resumeAnalysisService.saveAnalysis(
                    candidateId,
                    jobOpeningId,
                    fullName,
                    email,
                    atsScore,
                    aiAnalysis
            );

            return atsScore;

        } catch (Exception e) {

            log.error("Resume processing failed", e);

            throw new RuntimeException(e);
        }
    }
}