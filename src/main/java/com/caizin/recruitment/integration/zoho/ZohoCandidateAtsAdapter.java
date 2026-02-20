package com.caizin.recruitment.integration.zoho;

import com.caizin.recruitment.config.ZohoProperties;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.entity.Candidate;
import com.caizin.recruitment.exception.IntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    public void uploadScreeningQuestionsToApplication(String zohoCandidateId,
                                                      String zohoJobId,
                                                      List<ScreeningQuestionDto> questions) {
        try {
            String accessToken = tokenProvider.getAccessToken();

            // Step 1: Get Application ID
            String applicationId = getApplicationId(zohoCandidateId, zohoJobId, accessToken);
            log.info("Found application ID {} for candidate {}", applicationId, zohoCandidateId);

            // Step 2: Format questions as readable text
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < questions.size(); i++) {
                ScreeningQuestionDto q = questions.get(i);
                content.append(i + 1).append(". [").append(q.getType()).append("]\n");
                content.append(q.getQuestion()).append("\n\n");
            }

            String bodyJson = """
    {
      "data": [
        {
          "MulAI_Screening_Questionsti_Line_1": %s
        }
      ]
    }
    """.formatted(objectMapper.writeValueAsString(content.toString()));

            HttpUrl url = HttpUrl.parse(properties.getBaseUrl())
                    .newBuilder()
                    .addPathSegments("recruit/v2/Applications")
                    .addPathSegment(applicationId)
                    .build();

            log.info("Uploading screening questions to application {} via URL: {}", applicationId, url);

            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String raw = response.body() != null ? response.body().string() : "";
                log.info("Upload screening questions response: {}", raw);

                if (!response.isSuccessful()) {
                    throw new IntegrationException(
                            "Failed to upload screening questions. HTTP "
                                    + response.code() + " Body: " + raw);
                }

                log.info("Screening questions uploaded to application {} for candidate {}",
                        applicationId, zohoCandidateId);
            }

        } catch (IOException e) {
            throw new IntegrationException("Error uploading screening questions", e);
        }
    }

    private String getApplicationId(String zohoCandidateId,
                                    String zohoJobId,
                                    String accessToken) throws IOException {

        HttpUrl url = HttpUrl.parse(properties.getBaseUrl())
                .newBuilder()
                .addPathSegments("recruit/v2/Applications")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            log.info("Get application response: {}", raw);

            if (!response.isSuccessful()) {
                throw new IntegrationException(
                        "Failed to get application ID. HTTP "
                                + response.code() + " Body: " + raw);
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode dataNode = root.get("data");

            if (dataNode == null || !dataNode.isArray() || dataNode.size() == 0) {
                throw new IntegrationException(
                        "No application found for candidate " + zohoCandidateId);
            }

            // Find application matching both candidate and job
            for (JsonNode app : dataNode) {
                String candidateId = app.get("$Candidate_Id").asText();
                String jobId = app.get("$Job_Opening_Id").asText();

                if (zohoCandidateId.equals(candidateId) && zohoJobId.equals(jobId)) {
                    return app.get("id").asText();
                }
            }

            throw new IntegrationException("No application found for candidate " + zohoCandidateId);
        }
    }

}
