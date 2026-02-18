package com.caizin.recruitment.integration.keka;

import com.caizin.recruitment.config.KekaProperties;
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
 * KEKA adapter: API calls + transformation only.
 * No business logic should be placed here.
 */
@Component
@ConditionalOnProperty(prefix = "ats", name = "provider", havingValue = "keka")
public class KekaAtsAdapter implements AtsPlatform {
    private static final Logger log = LoggerFactory.getLogger(KekaAtsAdapter.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final KekaProperties properties;

    public KekaAtsAdapter(OkHttpClient httpClient, ObjectMapper objectMapper, KekaProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public List<JobDto> fetchJobs() {
        String baseUrl = trimToNull(properties.getBaseUrl());
        if (baseUrl == null) {
            throw new IntegrationException("KEKA base URL is not configured (keka.base-url)");
        }

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(baseUrl))
                .newBuilder()
                .addPathSegments("api/v1/hire/jobs")
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .get()
                .url(url)
                .addHeader("Accept", "application/json");

        String apiKey = trimToNull(properties.getApiKey());
        if (apiKey != null) {
            // Common auth pattern; can be adjusted without changing service/business logic.
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        } else {
            log.warn("KEKA api key is not configured (keka.api-key). Request may fail with 401.");
        }

        Request request = requestBuilder.build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body == null ? "" : body.string();

            if (!response.isSuccessful()) {
                throw new IntegrationException("Failed to fetch jobs from KEKA. HTTP " + response.code() + ". Body: " + abbreviate(raw, 2000));
            }

            if (raw.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(raw);
            JsonNode jobsNode = extractJobsNode(root);
            if (jobsNode == null || !jobsNode.isArray()) {
                log.warn("Unexpected KEKA jobs payload shape. Returning empty list.");
                return Collections.emptyList();
            }

            List<JobDto> jobs = new ArrayList<>();
            for (JsonNode jobNode : jobsNode) {
                jobs.add(mapJob(jobNode));
            }
            return jobs;
        } catch (IOException e) {
            throw new IntegrationException("IO error while calling KEKA jobs API", e);
        }
    }

    private JobDto mapJob(JsonNode node) {
        // Keep mapping tolerant to KEKA schema variations.
        String jobId = firstNonBlank(
                text(node, "id"),
                text(node, "jobId"),
                text(node, "job_id")
        );

        String title = firstNonBlank(
                text(node, "title"),
                text(node, "jobTitle"),
                text(node, "job_title")
        );

        String description = firstNonBlank(
                text(node, "description"),
                text(node, "jobDescription"),
                text(node, "job_description")
        );

        String department = firstNonBlank(
                text(node, "department"),
                text(node, "departmentName"),
                text(node, "department_name")
        );

        String experience = firstNonBlank(
                text(node, "experience"),
                text(node, "experienceLevel"),
                text(node, "experience_level"),
                text(node, "minExperience") // sometimes split into ranges
        );

        return new JobDto(jobId, title, description, experience, department);
    }

    private static JsonNode extractJobsNode(JsonNode root) {
        if (root == null) return null;
        if (root.isArray()) return root;
        JsonNode data = root.get("data");
        if (data != null && data.isArray()) return data;
        JsonNode jobs = root.get("jobs");
        if (jobs != null && jobs.isArray()) return jobs;
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return trimToNull(s);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            String t = trimToNull(v);
            if (t != null) return t;
        }
        return null;
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

