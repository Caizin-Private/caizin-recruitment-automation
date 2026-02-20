package com.caizin.recruitment.controller;

import com.caizin.recruitment.service.ZohoCandidateApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/zoho")
public class ZohoCandidateController {

    private final ZohoCandidateApplicationService zohoCandidateService;

    public ZohoCandidateController(
            ZohoCandidateApplicationService zohoCandidateService
    ) {
        this.zohoCandidateService = zohoCandidateService;
    }

    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> pushCandidatesToZoho() {

        int pushedCount = zohoCandidateService.pushProcessedCandidatesToZoho();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "pushedCount", pushedCount,
                "message", pushedCount + " candidates pushed to Zoho successfully."
        ));
    }
}