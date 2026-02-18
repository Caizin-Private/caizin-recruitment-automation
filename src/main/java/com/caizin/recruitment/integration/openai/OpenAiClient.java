package com.caizin.recruitment.integration.openai;

import com.caizin.recruitment.config.OpenAiProperties;
import com.caizin.recruitment.dto.OpenAiRequestDto;
import com.caizin.recruitment.dto.OpenAiResponseDto;
import com.caizin.recruitment.exception.OpenAiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * OpenAI client: REST call only (no prompt/business logic).
 */
@Component
public class OpenAiClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;

    public OpenAiClient(OkHttpClient httpClient, ObjectMapper objectMapper, OpenAiProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public OpenAiResponseDto createChatCompletion(OpenAiRequestDto requestDto) {
        String baseUrl = trimToNull(properties.getBaseUrl());
        if (baseUrl == null) {
            throw new OpenAiException("OpenAI base URL is not configured (openai.base-url)");
        }
        String apiKey = trimToNull(properties.getApiKey());
        if (apiKey == null) {
            throw new OpenAiException("OpenAI api key is not configured (openai.api-key / OPENAI_API_KEY)");
        }

        String url = baseUrl.endsWith("/") ? (baseUrl + "chat/completions") : (baseUrl + "/chat/completions");

        try {
            String json = objectMapper.writeValueAsString(requestDto);
            RequestBody body = RequestBody.create(json, JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                String raw = responseBody == null ? "" : responseBody.string();

                if (!response.isSuccessful()) {
                    throw new OpenAiException("OpenAI request failed. HTTP " + response.code() + ". Body: " + abbreviate(raw, 2000));
                }
                if (raw.isBlank()) {
                    throw new OpenAiException("OpenAI returned an empty response body");
                }
                return objectMapper.readValue(raw, OpenAiResponseDto.class);
            }
        } catch (IOException e) {
            log.error("OpenAI call failed", e);
            throw new OpenAiException("IO error while calling OpenAI", e);
        }
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

