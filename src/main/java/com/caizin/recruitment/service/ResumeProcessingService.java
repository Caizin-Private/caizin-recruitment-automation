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
            String senderEmail
    ) {

        try {

            log.info("=======================================");
            log.info("Starting resume processing: {}", file.getName());
            log.info("=======================================");

            // Step 1: Extract resume text
            String resumeText =
                    extractor.extractText(file);

            log.info("Resume text extracted successfully");

            // Step 2: Parse resume
            ParsedResume parsedResume =
                    resumeParser.parse(resumeText);

            log.info("Resume parsed successfully");

            // Step 3: Resolve candidate email
            String email =
                    parsedResume.email() == null ||
                            parsedResume.email().isBlank() ||
                            parsedResume.email().equalsIgnoreCase("unknown@email.com")
                            ? senderEmail
                            : parsedResume.email();

            // Step 4: Resolve candidate name
            String fullName =
                    parsedResume.fullName() == null ||
                            parsedResume.fullName().isBlank() ||
                            parsedResume.fullName().equalsIgnoreCase("UNKNOWN")
                            ? senderName
                            : parsedResume.fullName();

            log.info("Candidate Name: {}", fullName);
            log.info("Candidate Email: {}", email);

            // Step 5: Extract JD text
            String jdText =
                    jdExtractor.getJDText();

            log.info("JD text extracted successfully");

            // Step 6: Parse JD
            JDRequirements jdRequirements =
                    jdParser.parse(jdText);

            log.info("JD parsed successfully");

            // Step 7: Calculate ATS score
            double atsScore =
                    atsScoringService.calculate(
                            resumeText,
                            jdText,
                            parsedResume,
                            jdRequirements
                    );

            log.info("ATS Score calculated: {}", atsScore);

            // Step 8: Call MCP Server for AI analysis
            log.info("Calling MCP server for AI analysis...");

            Map<String, Object> aiAnalysis =
                    mcpClient.analyzeResume(
                            resumeText,
                            jdText
                    );

            log.info("AI analysis received successfully");

            // Step 9: Extract AI scores safely
            Integer technicalScore =
                    getInteger(aiAnalysis, "technical_score");

            Integer experienceScore =
                    getInteger(aiAnalysis, "experience_score");

            Integer communicationScore =
                    getInteger(aiAnalysis, "communication_score");

            Integer leadershipScore =
                    getInteger(aiAnalysis, "leadership_score");

            log.info(
                    "AI Scores → Technical: {}, Experience: {}, Communication: {}, Leadership: {}",
                    technicalScore,
                    experienceScore,
                    communicationScore,
                    leadershipScore
            );

            log.info("Skills detected: {}", aiAnalysis.get("skills"));
            log.info("Missing skills: {}", aiAnalysis.get("missing_skills"));
            log.info("Risk flags: {}", aiAnalysis.get("risk_flags"));

            // Step 10: Save analysis to database
            resumeAnalysisService.saveAnalysis(
                    fullName,
                    email,
                    atsScore,
                    aiAnalysis
            );

            log.info("AI analysis saved to database successfully");

            log.info("=======================================");
            log.info("Resume processing completed successfully for {} ({})",
                    fullName,
                    email);
            log.info("=======================================");

            return atsScore;

        } catch (Exception e) {

            log.error("Resume processing failed", e);

            throw new RuntimeException(
                    "Resume processing failed for file: " + file.getName(),
                    e
            );
        }
    }

    // Safe integer extraction helper
    private Integer getInteger(
            Map<String, Object> map,
            String key
    ) {

        Object value = map.get(key);

        if (value instanceof Integer)
            return (Integer) value;

        if (value instanceof Number)
            return ((Number) value).intValue();

        return 0;
    }
}