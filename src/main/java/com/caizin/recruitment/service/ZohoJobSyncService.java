package com.caizin.recruitment.service;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.caizin.recruitment.repository.CandidateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZohoJobSyncService {

    private final AtsPlatform atsPlatform;
    private final CandidateRepository candidateRepository;

    public ZohoJobSyncService(
            AtsPlatform atsPlatform,
            CandidateRepository candidateRepository
    ) {
        this.atsPlatform = atsPlatform;
        this.candidateRepository = candidateRepository;
    }

    public void syncZohoJobIdsToCandidates() {

        // 1️⃣ Fetch all Zoho jobs
        List<JobDto> jobs = atsPlatform.fetchJobs();

        if (jobs.isEmpty()) {
            return;
        }

        // 2️⃣ Fetch candidates where zohoJobId is null
        List<Candidate> candidates =
                candidateRepository.findByZohoJobIdIsNull();

        for (Candidate candidate : candidates) {

            for (JobDto job : jobs) {

                if (candidate.getJobOpeningId() != null &&
                        candidate.getJobOpeningId().equals(job.getJobOpeningId())) {

                    candidate.setZohoJobId(job.getZohoJobid());
                    candidateRepository.save(candidate);
                    break;
                }
            }
        }
    }
}
