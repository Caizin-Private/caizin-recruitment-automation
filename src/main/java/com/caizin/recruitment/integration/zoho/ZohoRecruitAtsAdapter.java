package com.caizin.recruitment.integration.zoho;

import com.caizin.recruitment.config.ZohoProperties;
import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.exception.IntegrationException;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Zoho Recruit adapter: API calls + transformation only.
 * No business logic should be placed here.
 */
@Component
@ConditionalOnProperty(prefix = "ats", name = "provider", havingValue = "zoho")
public class ZohoRecruitAtsAdapter implements AtsPlatform {
    private static final Logger log = LoggerFactory.getLogger(ZohoRecruitAtsAdapter.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ZohoProperties properties;
    private final ZohoAccessTokenProvider accessTokenProvider;

    public ZohoRecruitAtsAdapter(
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            ZohoProperties properties,
            ZohoAccessTokenProvider accessTokenProvider
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.accessTokenProvider = Objects.requireNonNull(accessTokenProvider, "accessTokenProvider");
    }

    @Override
    public List<JobDto> fetchJobs() {
        String baseUrl = trimToNull(properties.getBaseUrl());
        if (baseUrl == null) {
            throw new IntegrationException("Zoho base URL is not configured (zoho.base-url)");
        }

        String accessToken = accessTokenProvider.getAccessToken();

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(baseUrl))
                .newBuilder()
                .addPathSegments("recruit/v2/JobOpenings")
                .build();
        log.info("Calling Zoho Recruit JobOpenings API...");

        Request request = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Zoho-oauthtoken " + accessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body == null ? "" : body.string();

            if (!response.isSuccessful()) {
                throw new IntegrationException("Failed to fetch jobs from Zoho Recruit. HTTP " + response.code() + ". Body: " + abbreviate(raw, 2000));
            }

            if (raw.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode jobsNode = root == null ? null : root.get("data");
            if (jobsNode == null || !jobsNode.isArray()) {
                log.warn("Unexpected Zoho Recruit jobs payload shape. Returning empty list.");
                return Collections.emptyList();
            }

            List<JobDto> jobs = new ArrayList<>();
            for (JsonNode jobNode : jobsNode) {
                jobs.add(mapJob(jobNode));
            }
            log.info("Fetched {} jobs from Zoho", jobs.size());

            return jobs;
        } catch (IOException e) {
            throw new IntegrationException("IO error while calling Zoho Recruit jobs API", e);
        }
    }

    private static JobDto mapJob(JsonNode node) {
        String id = text(node, "id");
        String jobOpeningId = text(node, "Job_Opening_ID");
        String title = text(node, "Posting_Title");
        String description = text(node, "Job_Description");
        String experience = text(node, "Work_Experience");
        String department = text(node, "Industry");
        log.debug("Mapped Zoho job id={}, title={}", id, title);
        log.debug("Mapped Zoho job internalId={}, jobOpeningId={}", id, jobOpeningId);

        return new JobDto(id,jobOpeningId, title, description, experience, department);
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return trimToNull(s);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }
}

