package com.caizin.recruitment.integration.zoho;

import com.caizin.recruitment.config.ZohoProperties;
import com.caizin.recruitment.exception.IntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

/**
 * Thin client for Zoho Accounts OAuth token refresh.
 */
@Component
public class ZohoOAuthTokenClient {
    private static final Logger log = LoggerFactory.getLogger(ZohoOAuthTokenClient.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ZohoProperties properties;

    public ZohoOAuthTokenClient(OkHttpClient httpClient, ObjectMapper objectMapper, ZohoProperties properties) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public TokenResponse refreshAccessToken() {
        String accountsBaseUrl = trimToNull(properties.getAccountsBaseUrl());
        String clientId = trimToNull(properties.getClientId());
        String clientSecret = trimToNull(properties.getClientSecret());
        String refreshToken = trimToNull(properties.getRefreshToken());

        if (accountsBaseUrl == null) {
            throw new IntegrationException("Zoho accounts base URL is not configured (zoho.accounts-base-url)");
        }
        if (clientId == null || clientSecret == null || refreshToken == null) {
            throw new IntegrationException("Zoho OAuth credentials are not configured (zoho.client-id, zoho.client-secret, zoho.refresh-token)");
        }

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(accountsBaseUrl))
                .newBuilder()
                .addPathSegments("oauth/v2/token")
                .build();

        RequestBody form = new FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "refresh_token")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(form)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String raw = body == null ? "" : body.string();

            if (!response.isSuccessful()) {
                throw new IntegrationException("Failed to refresh Zoho access token. HTTP " + response.code() + ". Body: " + abbreviate(raw, 2000));
            }
            if (raw.isBlank()) {
                throw new IntegrationException("Zoho token refresh returned an empty response body");
            }

            JsonNode root = objectMapper.readTree(raw);
            String accessToken = text(root, "access_token");
            Long expiresInSec = longValue(root, "expires_in");
            if (expiresInSec == null) {
                // Zoho may return expires_in_sec depending on region/config.
                expiresInSec = longValue(root, "expires_in_sec");
            }

            if (accessToken == null) {
                throw new IntegrationException("Zoho token refresh response missing access_token");
            }

            long ttl = (expiresInSec == null || expiresInSec <= 0) ? 3600L : expiresInSec;
            Instant expiresAt = Instant.now().plusSeconds(ttl);

            log.info("Refreshed Zoho access token. Expires in {} seconds.", ttl);
            return new TokenResponse(accessToken, expiresAt);
        } catch (IOException e) {
            throw new IntegrationException("IO error while refreshing Zoho access token", e);
        }
    }

    public record TokenResponse(String accessToken, Instant expiresAt) { }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return trimToNull(s);
    }

    private static Long longValue(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asLong();
        String s = v.asText();
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
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

