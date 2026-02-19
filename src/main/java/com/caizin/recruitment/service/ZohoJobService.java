package com.caizin.recruitment.service;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZohoJobService {

    private final AtsPlatform atsPlatform;

    public ZohoJobService(AtsPlatform atsPlatform) {
        this.atsPlatform = atsPlatform;
    }

    public List<JobDto> fetchAllJobs() {
        return atsPlatform.fetchJobs();
    }
}
