package com.caizin.recruitment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    private String candidateId;

    @Column(name = "zoho_candidate_id")
    private String zohoCandidateId;

    @Column(name = "job_opening_id")
    private String jobOpeningId;   // ZR_1_JOB

    @Column(name = "zoho_job_id")  //
    private String zohoJobId;

    private String fullName;

    @Column(unique = true)
    private String email;

    // SharePoint file info
    private String sharepointSiteId;

    private String sharepointDriveId;

    private String sharepointItemId;

    private String fileName;

    private String source;

    private String status;

    private String createdAt;

    private Double atsScore;



    public static Candidate create(
            String fullName,
            String email,
            String siteId,
            String driveId,
            String itemId,
            String fileName,
            String source,
            String status
    ) {
        Candidate c = new Candidate();

        c.candidateId = UUID.randomUUID().toString();
        c.zohoCandidateId = null;
        c.fullName = fullName;
        c.email = email;
        c.sharepointSiteId = siteId;
        c.sharepointDriveId = driveId;
        c.sharepointItemId = itemId;
        c.fileName = fileName;
        c.source = source;
        c.status = status;
        c.createdAt = Instant.now().toString();
        c.atsScore = null;

        return c;
    }
}