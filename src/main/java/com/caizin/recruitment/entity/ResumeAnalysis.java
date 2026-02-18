package com.caizin.recruitment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_analysis")
@Data
public class ResumeAnalysis {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "experience_score")
    private Integer experienceScore;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "leadership_score")
    private Integer leadershipScore;

    @Column(name = "ats_score")
    private Double atsScore;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "risk_flags", columnDefinition = "TEXT")
    private String riskFlags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ResumeAnalysis() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }
}