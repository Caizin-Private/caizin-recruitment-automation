package com.caizin.recruitment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zoho")
public class ZohoProperties {
    private String baseUrl;
    /**
     * Optional static access token. Prefer refresh-token based flow in production.
     */
    private String accessToken;

    /**
     * Zoho Accounts base URL, e.g. https://accounts.zoho.in
     */
    private String accountsBaseUrl;

    private String clientId;
    private String clientSecret;
    private String refreshToken;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccountsBaseUrl() {
        return accountsBaseUrl;
    }

    public void setAccountsBaseUrl(String accountsBaseUrl) {
        this.accountsBaseUrl = accountsBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

