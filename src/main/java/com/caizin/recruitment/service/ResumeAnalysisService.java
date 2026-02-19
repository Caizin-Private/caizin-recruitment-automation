package com.caizin.recruitment.service;

import com.caizin.recruitment.entity.ResumeAnalysis;
import com.caizin.recruitment.repository.ResumeAnalysisRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ResumeAnalysisService {

    private final ResumeAnalysisRepository repository;

    public ResumeAnalysisService(
            ResumeAnalysisRepository repository
    ) {
        this.repository = repository;
    }

    public ResumeAnalysis saveAnalysis(
            String candidateId,
            String jobId,
            String candidateName,
            String candidateEmail,
            Double atsScore,
            Map<String, Object> aiAnalysis
    ) {

        ResumeAnalysis analysis = new ResumeAnalysis();

        analysis.setCandidateId(candidateId);
        analysis.setJobId(jobId);

        analysis.setCandidateName(candidateName);
        analysis.setCandidateEmail(candidateEmail);

        analysis.setAtsScore(atsScore);

        analysis.setTechnicalScore(getInt(aiAnalysis, "technical_score"));
        analysis.setExperienceScore(getInt(aiAnalysis, "experience_score"));
        analysis.setCommunicationScore(getInt(aiAnalysis, "communication_score"));
        analysis.setLeadershipScore(getInt(aiAnalysis, "leadership_score"));

        analysis.setSkills(aiAnalysis.getOrDefault("skills", "").toString());
        analysis.setMissingSkills(aiAnalysis.getOrDefault("missing_skills", "").toString());
        analysis.setRiskFlags(aiAnalysis.getOrDefault("risk_flags", "").toString());

        return repository.save(analysis);
    }

    private Integer getInt(Map<String, Object> map, String key) {

        Object value = map.get(key);

        if (value instanceof Number)
            return ((Number) value).intValue();

        return 0;
    }
}