package com.caizin.recruitment.integration.MCPClient;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MCPClient {

    private final WebClient webClient;

    public MCPClient() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:3001")
                .build();
    }

    public Map analyzeResume(String resumeText, String jobDescription) {

        Map<String, String> requestBody = Map.of(
                "resumeText", resumeText,
                "jobDescription", jobDescription
        );

        return webClient.post()
                .uri("/tools/analyze_resume")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}