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

        return webClient.post()
                .uri("/tools/analyze_resume")
                .bodyValue(Map.of(
                        "resume_text", resumeText,
                        "job_description", jobDescription
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}