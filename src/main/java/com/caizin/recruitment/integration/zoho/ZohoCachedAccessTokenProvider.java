package com.caizin.recruitment.integration.zoho;

import com.caizin.recruitment.config.ZohoProperties;
import com.caizin.recruitment.exception.IntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Access token provider with in-memory caching and automatic refresh.
 *
 * <p>Uses a static access token if configured, otherwise refreshes using Zoho Accounts refresh_token flow.</p>
 */
@Component
public class ZohoCachedAccessTokenProvider implements ZohoAccessTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(ZohoCachedAccessTokenProvider.class);
    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(60);

    private final ZohoProperties properties;
    private final ZohoOAuthTokenClient tokenClient;

    private volatile String cachedAccessToken;
    private volatile Instant cachedExpiresAt;

    public ZohoCachedAccessTokenProvider(ZohoProperties properties, ZohoOAuthTokenClient tokenClient) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.tokenClient = Objects.requireNonNull(tokenClient, "tokenClient");
    }

    @Override
    public String getAccessToken() {
        // If an access token is explicitly provided, use it as-is (useful for manual testing).
        String configuredToken = trimToNull(properties.getAccessToken());
        if (configuredToken != null) {
            return configuredToken;
        }

        String token = cachedAccessToken;
        Instant expiresAt = cachedExpiresAt;
        if (token != null && expiresAt != null && Instant.now().isBefore(expiresAt.minus(EXPIRY_SKEW))) {
            return token;
        }

        synchronized (this) {
            token = cachedAccessToken;
            expiresAt = cachedExpiresAt;
            if (token != null && expiresAt != null && Instant.now().isBefore(expiresAt.minus(EXPIRY_SKEW))) {
                return token;
            }

            ZohoOAuthTokenClient.TokenResponse refreshed = tokenClient.refreshAccessToken();
            String refreshedToken = trimToNull(refreshed.accessToken());
            if (refreshedToken == null) {
                throw new IntegrationException("Zoho token refresh returned a blank access token");
            }

            cachedAccessToken = refreshedToken;
            cachedExpiresAt = refreshed.expiresAt();
            log.debug("Cached Zoho access token until {}", cachedExpiresAt);
            return refreshedToken;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

