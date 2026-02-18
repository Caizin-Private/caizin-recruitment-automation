package com.caizin.recruitment.service;

import com.caizin.recruitment.entity.JDRequirements;
import com.caizin.recruitment.entity.ParsedResume;
import com.caizin.recruitment.parser.JDParser;
import com.caizin.recruitment.parser.ResumeParser;
import com.caizin.recruitment.util.JDTextExtractor;
import com.caizin.recruitment.util.ResumeTextExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ResumeProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeProcessingService.class);

    private final ResumeTextExtractor extractor;
    private final ResumeParser resumeParser;
    private final JDParser jdParser;
    private final JDTextExtractor jdExtractor;
    private final ATSScoringService atsScoringService;

    public ResumeProcessingService(
            ResumeTextExtractor extractor,
            ResumeParser resumeParser,
            JDParser jdParser,
            JDTextExtractor jdExtractor,
            ATSScoringService atsScoringService
    ) {
        this.extractor = extractor;
        this.resumeParser = resumeParser;
        this.jdParser = jdParser;
        this.jdExtractor = jdExtractor;
        this.atsScoringService = atsScoringService;
    }

    public double process(
            File file,
            String senderName,
            String senderEmail
    ) {

        try {

            String resumeText =
                    extractor.extractText(file);

            log.info("Resume text extracted");

            ParsedResume parsedResume =
                    resumeParser.parse(resumeText);

            log.info("Resume parsed successfully");

            String email =
                    parsedResume.email().equals("unknown@email.com")
                            ? senderEmail
                            : parsedResume.email();

            String fullName =
                    (parsedResume.fullName() == null ||
                            parsedResume.fullName().isBlank() ||
                            parsedResume.fullName().equalsIgnoreCase("UNKNOWN"))
                            ? senderName
                            : parsedResume.fullName();

            log.info("Candidate Name: {}", fullName);
            log.info("Candidate Email: {}", email);

            String jdText =
                    jdExtractor.getJDText();

            log.info("JD text extracted");

            JDRequirements jdRequirements =
                    jdParser.parse(jdText);

            log.info("JD parsed successfully");

            double atsScore =
                    atsScoringService.calculate(
                            resumeText,
                            jdText,
                            parsedResume,
                            jdRequirements
                    );

            log.info("ATS Score calculated: {}", atsScore);

            log.info("Resume processed successfully for {} ({})",
                    fullName,
                    email);

            return atsScore;

        } catch (Exception e) {

            log.error("Error processing resume", e);

            throw new RuntimeException(e);
        }
    }
}