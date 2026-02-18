package com.caizin.recruitment.controller;

import com.caizin.recruitment.service.SharePointService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sharepoint")
public class SharePointController {

    private static final Logger log =
            LoggerFactory.getLogger(SharePointController.class);

    private final SharePointService sharePointService;

    public SharePointController(
            SharePointService sharePointService
    ) {
        this.sharePointService = sharePointService;
    }


    @PostMapping("/process")
    public String processResumes() {

        log.info("Manual SharePoint resume processing started");

        sharePointService.processResumes();

        return "SharePoint resume processing completed.";
    }


    @GetMapping("/health")
    public String health() {

        return "SharePoint service is running.";
    }
}