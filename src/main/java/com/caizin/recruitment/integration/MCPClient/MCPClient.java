package com.caizin.recruitment.integration.MCPClient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Component
public class MCPClient {

    private final WebClient webClient;

    public MCPClient() {

        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:3001")
                .build();
    }

    public Map<String, Object> analyzeResume(
            String resumeText,
            String jobDescription,
            String jobId,
            String candidateId
    ) {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("resumeText", resumeText);
        requestBody.put("jobDescription", jobDescription);
        requestBody.put("jobId", jobId);
        requestBody.put("candidateId", candidateId);

        return webClient.post()
                .uri("/tools/analyze_resume")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}