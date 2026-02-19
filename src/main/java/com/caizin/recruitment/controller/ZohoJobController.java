package com.caizin.recruitment.controller;

import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.service.ZohoJobService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ats/jobs")
public class ZohoJobController {

    private final ZohoJobService zohoJobService;

    public ZohoJobController(ZohoJobService zohoJobService) {
        this.zohoJobService = zohoJobService;
    }

    @GetMapping
    public List<JobDto> getAllJobs() {
        return zohoJobService.fetchAllJobs();
    }
}
