package com.caizin.recruitment.integration.zoho;

import com.caizin.recruitment.config.ZohoProperties;
import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.exception.IntegrationException;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "ats", name = "provider", havingValue = "zoho")
public class ZohoCandidateAtsAdapter {

    private static final Logger log = LoggerFactory.getLogger(ZohoCandidateAtsAdapter.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ZohoProperties properties;
    private final ZohoAccessTokenProvider tokenProvider;

    public ZohoCandidateAtsAdapter(
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            ZohoProperties properties,
            ZohoAccessTokenProvider tokenProvider
    ) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
    }

    public String createCandidate(Candidate candidate) {

        try {
            String accessToken = tokenProvider.getAccessToken();

            String[] nameParts = candidate.getFullName().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "NA";

            String bodyJson = """
                {
                  "data": [
                    {
                      "First_Name": "%s",
                      "Last_Name": "%s",
                      "Email": "%s"
                    }
                  ]
                }
                """.formatted(firstName, lastName, candidate.getEmail());

            HttpUrl url = HttpUrl.parse(properties.getBaseUrl())
                    .newBuilder()
                    .addPathSegments("recruit/v2/Candidates")
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {

                String raw = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    throw new IntegrationException("Failed to create candidate. HTTP "
                            + response.code() + " Body: " + raw);
                }

                JsonNode root = objectMapper.readTree(raw);
                String candidateId = root.get("data").get(0).get("details").get("id").asText();

                log.info("Created Zoho candidate with ID {}", candidateId);
                return candidateId;
            }

        } catch (IOException e) {
            throw new IntegrationException("Error creating Zoho candidate", e);
        }
    }

    public void uploadResume(String zohoCandidateId, File resumeFile) {

        if (zohoCandidateId == null || zohoCandidateId.isBlank()) {
            throw new IntegrationException("Zoho candidate ID is blank");
        }

        if (!resumeFile.exists()) {
            throw new IntegrationException("Resume file does not exist: " + resumeFile.getAbsolutePath());
        }

        String accessToken = tokenProvider.getAccessToken();

        HttpUrl url = HttpUrl.parse(properties.getBaseUrl())
                .newBuilder()
                .addPathSegments("recruit/v2/Candidates")
                .addPathSegment(zohoCandidateId)
                .addPathSegment("Attachments")
                .build();

        RequestBody fileBody = RequestBody.create(
                resumeFile,
                okhttp3.MediaType.parse("application/pdf")
        );

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        resumeFile.getName(),
                        fileBody
                )
                .addFormDataPart(
                        "attachments_category",   // 🔥 REQUIRED
                        "Resume"                  // Valid category
                )
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            String raw = response.body() != null
                    ? response.body().string()
                    : "";

            if (!response.isSuccessful()) {
                throw new IntegrationException(
                        "Failed to upload resume. HTTP "
                                + response.code()
                                + ". Body: "
                                + raw
                );
            }

            log.info("Resume uploaded successfully for candidate {}", zohoCandidateId);

        } catch (IOException e) {
            throw new IntegrationException("Error uploading resume", e);
        }
    }


    public void associateWithJob(String zohoCandidateId, String zohoJobId) {

        if (zohoCandidateId == null || zohoCandidateId.isBlank()) {
            throw new IntegrationException("Zoho candidate ID is blank");
        }
        if (zohoJobId == null || zohoJobId.isBlank()) {
            throw new IntegrationException("Zoho job ID is blank for candidate " + zohoCandidateId);
        }

        try {
            String accessToken = tokenProvider.getAccessToken();

            // Both IDs go in the body — NOT in the URL
            String bodyJson = """
            {
              "data": [
                {
                  "ids": ["%s"],
                  "jobids": ["%s"]
                }
              ]
            }
            """.formatted(zohoCandidateId, zohoJobId);

            HttpUrl url = HttpUrl.parse(properties.getBaseUrl())
                    .newBuilder()
                    .addPathSegments("recruit/v2/Candidates")
                    .addPathSegment("actions")
                    .addPathSegment("associate")
                    .build();

            log.info("Associating candidate {} with job {} via URL: {}", zohoCandidateId, zohoJobId, url);

            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {

                String raw = response.body() != null
                        ? response.body().string()
                        : "";

                if (!response.isSuccessful()) {
                    throw new IntegrationException(
                            "Failed to associate candidate with job. HTTP "
                                    + response.code()
                                    + " Body: " + raw
                    );
                }

                log.info("Candidate {} successfully associated with job {}", zohoCandidateId, zohoJobId);
            }

        } catch (IOException e) {
            throw new IntegrationException("Error associating candidate with job", e);
        }
    }
}
