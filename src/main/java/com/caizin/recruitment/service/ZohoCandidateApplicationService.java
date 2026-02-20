package com.caizin.recruitment.service;

import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.integration.zoho.ZohoCandidateAtsAdapter;
import com.caizin.recruitment.repository.CandidateRepository;
import com.caizin.recruitment.service.pipeline.ScreeningQuestionsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ZohoCandidateApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ZohoCandidateApplicationService.class);

    private final CandidateRepository candidateRepository;
    private final ZohoCandidateAtsAdapter zohoAdapter;
    private final ZohoJobSyncService zohoJobSyncService;
    private final ScreeningQuestionsContext screeningQuestionsContext; // ← add this


    @Value("${sharepoint.download-dir}")
    private String downloadDir;

    public ZohoCandidateApplicationService(
            CandidateRepository candidateRepository,
            ZohoCandidateAtsAdapter zohoAdapter, ZohoJobSyncService zohoJobSyncService, ScreeningQuestionsContext screeningQuestionsContext
    ) {
        this.candidateRepository = candidateRepository;
        this.zohoAdapter = zohoAdapter;
        this.zohoJobSyncService = zohoJobSyncService;
        this.screeningQuestionsContext = screeningQuestionsContext;
    }

    public int pushProcessedCandidatesToZoho() {

        // 🔥 First sync job IDs
        zohoJobSyncService.syncZohoJobIdsToCandidates();

        List<ScreeningQuestionDto> questions = screeningQuestionsContext.getQuestions();

        List<Candidate> candidates =
                candidateRepository.findByStatusAndZohoCandidateIdIsNull("PROCESSED");

        int successCount = 0;

        for (Candidate candidate : candidates) {

            try {

                File resumeFile =
                        new File(downloadDir, candidate.getFileName());

                if (!resumeFile.exists()) {
                    throw new RuntimeException(
                            "Resume file not found for " + candidate.getEmail());
                }

                // 1️⃣ Create candidate in Zoho
                String zohoCandidateId =
                        zohoAdapter.createCandidate(candidate);

                // 2️⃣ Upload resume
                zohoAdapter.uploadResume(zohoCandidateId, resumeFile);

                // 3️⃣ Associate with job opening
                zohoAdapter.associateWithJob(
                        zohoCandidateId,
                        candidate.getZohoJobId()
                );
//

                // 4️⃣ Upload screening questions to application custom field
                if (screeningQuestionsContext.hasQuestions()) {
                    try {
                        zohoAdapter.uploadScreeningQuestionsToApplication(
                                zohoCandidateId,
                                candidate.getZohoJobId(),
                                questions
                        );
                    } catch (Exception e) {
                        log.warn("Failed to upload screening questions for {}: {}",
                                candidate.getEmail(), e.getMessage());
                    }
                }

                // 4️⃣ Update DB
                candidate.setZohoCandidateId(zohoCandidateId);
                candidate.setStatus("PUSHED_TO_ZOHO");

                candidateRepository.save(candidate);

                successCount++;

            } catch (Exception e) {
                System.err.println(
                        "Failed pushing candidate " + candidate.getEmail());
                e.printStackTrace();
            }
        }

        return successCount;
    }


}
