package com.caizin.recruitment.integration.ats;

import com.caizin.recruitment.dto.JobDto;

import java.util.List;

/**
 * Abstraction for ATS platforms (e.g. KEKA, Greenhouse, Lever).
 * Business logic must depend only on this interface.
 */
public interface AtsPlatform {
    List<JobDto> fetchJobs();
}

