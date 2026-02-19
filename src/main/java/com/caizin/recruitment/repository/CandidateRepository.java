package com.caizin.recruitment.repository;

import com.caizin.recruitment.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, String> {

    /**
     * Used for duplicate prevention
     */
    boolean existsBySharepointItemId(String sharepointItemId);

    /**
     * Fetch candidate by SharePoint item ID
     */
    Optional<Candidate> findBySharepointItemId(String sharepointItemId);

    /**
     * Fetch candidate by email (useful for queries)
     */
    Optional<Candidate> findByEmail(String email);
    Optional<Candidate> findTopByStatus(String status);

}