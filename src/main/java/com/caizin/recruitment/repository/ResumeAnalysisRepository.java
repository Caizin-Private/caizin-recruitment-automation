package com.caizin.recruitment.repository;

import com.caizin.recruitment.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResumeAnalysisRepository
        extends JpaRepository<ResumeAnalysis, UUID> {
}