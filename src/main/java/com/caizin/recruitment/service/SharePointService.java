package com.caizin.recruitment.service;

import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.entity.ParsedResume;
import com.caizin.recruitment.parser.ResumeParser;
import com.caizin.recruitment.repository.CandidateRepository;
import com.caizin.recruitment.util.ResumeTextExtractor;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class SharePointService {

    private static final Logger log =
            LoggerFactory.getLogger(SharePointService.class);

    private final GraphServiceClient graphClient;
    private final ResumeProcessingService resumeProcessingService;
    private final CandidateRepository candidateRepository;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeParser resumeParser;

    private final String driveId;
    private final String folderId;
    private final String downloadDir;


    public SharePointService(
            GraphServiceClient graphClient,
            ResumeProcessingService resumeProcessingService,
            CandidateRepository candidateRepository,
            ResumeTextExtractor resumeTextExtractor,
            ResumeParser resumeParser,
            @Value("${sharepoint.drive-id}") String driveId,
            @Value("${sharepoint.folder-id}") String folderId,
            @Value("${sharepoint.download-dir}") String downloadDir
    ) {

        this.graphClient = graphClient;
        this.resumeProcessingService = resumeProcessingService;
        this.candidateRepository = candidateRepository;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeParser = resumeParser;
        this.driveId = driveId;
        this.folderId = folderId;
        this.downloadDir = downloadDir;

        createDownloadDirectory();
    }


    /**
     * Ensure download directory exists
     */
    private void createDownloadDirectory() {

        File dir = new File(downloadDir);

        if (!dir.exists()) {

            boolean created = dir.mkdirs();

            if (created)
                log.info("Created download directory: {}", downloadDir);
        }
    }


    /**
     * Main method to fetch and process resumes
     */
    public void processResumes() {

        try {

            log.info("Fetching job folders from SharePoint...");

            DriveItemCollectionResponse folderResponse =
                    graphClient
                            .drives()
                            .byDriveId(driveId)
                            .items()
                            .byDriveItemId(folderId)
                            .children()
                            .get();

            List<DriveItem> folders = folderResponse.getValue();

            if (folders == null || folders.isEmpty()) {

                log.info("No job folders found.");
                return;
            }

            log.info("Found {} job folders", folders.size());

            for (DriveItem folder : folders) {

                // Skip files, process folders only
                if (folder.getFolder() == null)
                    continue;

                String jobId = folder.getName();

                log.info("Processing job folder: {}", jobId);

                DriveItemCollectionResponse fileResponse =
                        graphClient
                                .drives()
                                .byDriveId(driveId)
                                .items()
                                .byDriveItemId(folder.getId())
                                .children()
                                .get();

                List<DriveItem> files = fileResponse.getValue();

                if (files == null || files.isEmpty()) {

                    log.info("No resumes found in job folder {}", jobId);
                    continue;
                }

                for (DriveItem item : files) {

                    String itemId = item.getId();
                    String fileName = item.getName();

                    if (fileName == null)
                        continue;

                    if (!fileName.toLowerCase().endsWith(".pdf")) {

                        log.info("Skipping non-PDF file: {}", fileName);
                        continue;
                    }

                    if (candidateRepository.existsBySharepointItemId(itemId)) {

                        log.info("Already processed: {}", fileName);
                        continue;
                    }

                    log.info("Downloading resume {} for job {}", fileName, jobId);

                    File file = downloadFile(itemId, fileName);

                    String resumeText =
                            resumeTextExtractor.extractText(file);

                    ParsedResume parsedResume =
                            resumeParser.parse(resumeText);

                    String fullName =
                            parsedResume.fullName().equals("UNKNOWN")
                                    ? fallbackName(fileName)
                                    : parsedResume.fullName();

                    String email =
                            parsedResume.email().equals("unknown@email.com")
                                    ? fallbackEmail(fileName)
                                    : parsedResume.email();

                    Candidate candidate =
                            Candidate.create(
                                    fullName,
                                    email,
                                    item.getParentReference().getSiteId(),
                                    item.getParentReference().getDriveId(),
                                    itemId,
                                    fileName,
                                    "SHAREPOINT",
                                    "DOWNLOADED"
                            );

                    // IMPORTANT: SET JOB ID
                    candidate.setJobId(jobId);

                    candidateRepository.save(candidate);

                    log.info("Saved candidate for job {}", jobId);

                    double atsScore =
                            resumeProcessingService.process(
                                    file,
                                    fullName,
                                    email,
                                    jobId,
                                    candidate.getCandidateId()
                            );

                    candidate.setAtsScore(atsScore);
                    candidate.setStatus("PROCESSED");

                    candidateRepository.save(candidate);

                    log.info(
                            "ATS score {} saved for {} (job {})",
                            atsScore,
                            email,
                            jobId
                    );
                }
            }

            log.info("All job folders processed successfully.");

        } catch (Exception e) {

            log.error("Error processing SharePoint resumes", e);
        }
    }


    /**
     * Download file from SharePoint
     */
    private File downloadFile(
            String itemId,
            String fileName
    ) throws Exception {

        InputStream stream =
                graphClient
                        .drives()
                        .byDriveId(driveId)
                        .items()
                        .byDriveItemId(itemId)
                        .content()
                        .get();

        File file =
                new File(downloadDir, fileName);

        try (FileOutputStream output =
                     new FileOutputStream(file)) {

            stream.transferTo(output);
        }

        return file;
    }


    /**
     * Fallback name from filename
     */
    private String fallbackName(String fileName) {

        return fileName
                .replace(".pdf", "")
                .replace("_", " ")
                .trim();
    }


    /**
     * Fallback email from filename
     */
    private String fallbackEmail(String fileName) {

        if (fileName.contains("@"))
            return fileName.split(" ")[0];

        return fileName
                .replace(".pdf", "")
                .replace(" ", "")
                .toLowerCase()
                + "@unknown.com";
    }
}