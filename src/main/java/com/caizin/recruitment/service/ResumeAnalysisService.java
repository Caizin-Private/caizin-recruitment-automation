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
            String candidateName,
            String candidateEmail,
            Double atsScore,
            Map<String, Object> aiAnalysis
    ) {

        ResumeAnalysis analysis = new ResumeAnalysis();

        analysis.setCandidateName(candidateName);
        analysis.setCandidateEmail(candidateEmail);
        analysis.setAtsScore(atsScore);

        analysis.setTechnicalScore(
                getInteger(aiAnalysis, "technical_score"));

        analysis.setExperienceScore(
                getInteger(aiAnalysis, "experience_score"));

        analysis.setCommunicationScore(
                getInteger(aiAnalysis, "communication_score"));

        analysis.setLeadershipScore(
                getInteger(aiAnalysis, "leadership_score"));

        analysis.setSkills(
                aiAnalysis.get("skills").toString());

        analysis.setMissingSkills(
                aiAnalysis.get("missing_skills").toString());

        analysis.setRiskFlags(
                aiAnalysis.get("risk_flags").toString());

        return repository.save(analysis);
    }

    private Integer getInteger(
            Map<String, Object> map,
            String key
    ) {
        Object value = map.get(key);

        if (value instanceof Number)
            return ((Number) value).intValue();

        return 0;
    }
}