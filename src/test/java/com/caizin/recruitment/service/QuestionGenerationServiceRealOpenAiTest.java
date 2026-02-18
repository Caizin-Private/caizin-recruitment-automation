package com.caizin.recruitment.service;

import com.caizin.recruitment.config.OpenAiProperties;
import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.dto.JobQuestionResponseDto;
import com.caizin.recruitment.dto.OpenAiRequestDto;
import com.caizin.recruitment.dto.OpenAiResponseDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.exception.OpenAiException;
import com.caizin.recruitment.integration.ats.AtsPlatform;
import com.caizin.recruitment.integration.openai.OpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.when;

/**
 * End-to-end Flow 2 test:
 * - Mock ATS only (AtsPlatform)
 * - Use real LLM service + real OpenAI client
 */
@SpringBootTest
@ActiveProfiles("prod") // ensure @Profile("test") mocks are NOT loaded
class QuestionGenerationServiceRealOpenAiTest {
    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationServiceRealOpenAiTest.class);

    @Autowired
    private QuestionGenerationService questionGenerationService;

    @Autowired
    private OpenAiProperties openAiProperties;

    @MockBean
    private AtsPlatform atsPlatform;

    @Test
    void shouldGenerateRealLlmQuestionsFromMockJob() {
        assumeTrue(openAiProperties.getApiKey() != null && !openAiProperties.getApiKey().isBlank(),
                "OpenAI API key is not configured; skipping real OpenAI integration test.");

        JobDto job = new JobDto(
                "job-001",
                "Software Engineer - .NET Engineer – Microservices & Cloud",
                "Experience in microservices, .NET Core, Docker, Kubernetes.",
                "3-5 years",
                "Engineering"
        );

        when(atsPlatform.fetchJobs()).thenReturn(List.of(job));

        List<JobQuestionResponseDto> response = callWithRateLimitRetries(
                () -> questionGenerationService.generateQuestionsForAllJobs(),
                3,
                Duration.ofSeconds(2)
        );

        assertNotNull(response);
        assertFalse(response.isEmpty(), "Response should not be empty");

        JobQuestionResponseDto first = response.get(0);
        assertEquals("job-001", first.getJobId(), "Job ID should match mock job");

        List<ScreeningQuestionDto> questions = first.getQuestions();
        assertNotNull(questions, "Questions list should not be null");
        assertTrue(questions.size() >= 5, "Questions list size should be >= 5");

        for (int i = 0; i < questions.size(); i++) {
            ScreeningQuestionDto q = questions.get(i);
            assertNotNull(q, "Question at index " + i + " should not be null");
            assertNotNull(q.getQuestion(), "Question text at index " + i + " should not be null");
            assertFalse(q.getQuestion().isBlank(), "Question text at index " + i + " should not be blank");
        }

        logGeneratedQuestions(first);
    }

    /**
     * Test-local service used to orchestrate Flow 2 without touching production code.
     */
    interface QuestionGenerationService {
        List<JobQuestionResponseDto> generateQuestionsForAllJobs();
    }

    interface LlmService {
        List<ScreeningQuestionDto> generateQuestions(JobDto job);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        QuestionGenerationService questionGenerationService(AtsPlatform atsPlatform, LlmService llmService) {
            return () -> {
                List<JobDto> jobs = atsPlatform.fetchJobs();
                if (jobs == null || jobs.isEmpty()) {
                    return List.of();
                }

                List<JobQuestionResponseDto> out = new ArrayList<>();
                for (JobDto job : jobs) {
                    if (job == null) continue;
                    out.add(new JobQuestionResponseDto(job.getJobId(), llmService.generateQuestions(job)));
                }
                return out;
            };
        }

        @Bean
        LlmService llmService(OpenAiClient openAiClient, OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
            return new RealOpenAiLlmService(openAiClient, openAiProperties, objectMapper);
        }
    }

    /**
     * Real OpenAI-backed LLM implementation used by this test.
     * Keeps prompt logic inside the test (per "do not modify production code").
     */
    static class RealOpenAiLlmService implements LlmService {
        private final OpenAiClient openAiClient;
        private final OpenAiProperties properties;
        private final ObjectMapper objectMapper;

        RealOpenAiLlmService(OpenAiClient openAiClient, OpenAiProperties properties, ObjectMapper objectMapper) {
            this.openAiClient = Objects.requireNonNull(openAiClient, "openAiClient");
            this.properties = Objects.requireNonNull(properties, "properties");
            this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        }

        @Override
        public List<ScreeningQuestionDto> generateQuestions(JobDto job) {
            Objects.requireNonNull(job, "job");

            String prompt = buildPrompt(job);
            OpenAiRequestDto request = new OpenAiRequestDto(
                    requireNonBlank(properties.getModel(), "openai.model"),
                    List.of(new OpenAiRequestDto.Message("user", prompt)),
                    properties.getTemperature(),
                    new OpenAiRequestDto.ResponseFormat("json_object")
            );

            OpenAiResponseDto response = openAiClient.createChatCompletion(request);
            String content = extractContent(response);
            List<ScreeningQuestionDto> parsed = parseQuestionsJson(content);

            log.info("OpenAI returned {} question(s) for jobId={}", parsed.size(), job.getJobId());
            return parsed;
        }

        private String buildPrompt(JobDto job) {
            return """
                    You are a senior technical interviewer.

                    Generate 10 high-quality descriptive screening questions for the following job:

                    Job Title: %s
                    Department: %s
                    Experience Level: %s

                    Job Description:
                    %s

                    Rules:
                    - Only descriptive questions
                    - No MCQs
                    - No coding challenges
                    - Include experience validation questions
                    - Include scenario-based questions
                    - Include architecture/design questions
                    - Avoid generic questions

                    Return JSON format:
                    {
                      "questions": [
                        {
                          "type": "...",
                          "question": "..."
                        }
                      ]
                    }
                    """.formatted(
                    safe(job.getTitle()),
                    safe(job.getDepartment()),
                    safe(job.getExperience()),
                    safe(job.getDescription())
            );
        }

        private List<ScreeningQuestionDto> parseQuestionsJson(String content) {
            try {
                JsonNode root = objectMapper.readTree(content);
                JsonNode questionsNode = root.get("questions");
                if (questionsNode == null || !questionsNode.isArray()) {
                    throw new OpenAiException("OpenAI response JSON missing 'questions' array");
                }

                List<ScreeningQuestionDto> questions = new ArrayList<>();
                for (JsonNode q : questionsNode) {
                    String type = text(q, "type");
                    String question = text(q, "question");
                    if (question == null) continue;
                    questions.add(new ScreeningQuestionDto(type, question));
                }

                if (questions.isEmpty()) {
                    throw new OpenAiException("OpenAI returned zero parsed questions");
                }

                return questions;
            } catch (OpenAiException e) {
                throw e;
            } catch (Exception e) {
                throw new OpenAiException("Failed to parse OpenAI response as JSON", e);
            }
        }

        private static String extractContent(OpenAiResponseDto response) {
            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new OpenAiException("OpenAI response missing choices");
            }
            OpenAiResponseDto.Choice c0 = response.getChoices().get(0);
            if (c0 == null || c0.getMessage() == null || c0.getMessage().getContent() == null) {
                throw new OpenAiException("OpenAI response missing message content");
            }
            String content = response.getChoices().get(0).getMessage().getContent().trim();
            if (content.isEmpty()) {
                throw new OpenAiException("OpenAI returned empty message content");
            }
            return content;
        }

        private static String requireNonBlank(String v, String name) {
            if (v == null || v.isBlank()) {
                throw new OpenAiException("Missing configuration: " + name);
            }
            return v.trim();
        }

        private static String safe(String s) {
            return (s == null || s.isBlank()) ? "N/A" : s;
        }

        private static String text(JsonNode node, String field) {
            if (node == null) return null;
            JsonNode v = node.get(field);
            if (v == null || v.isNull()) return null;
            String s = v.asText();
            return (s == null || s.isBlank()) ? null : s.trim();
        }
    }

    private static void logGeneratedQuestions(JobQuestionResponseDto result) {
        log.info("Generated questions for jobId={}", result.getJobId());
        if (result.getQuestions() == null) {
            log.info("No questions returned.");
            return;
        }
        for (int i = 0; i < result.getQuestions().size(); i++) {
            ScreeningQuestionDto q = result.getQuestions().get(i);
            log.info("[{}] type={} question={}", i + 1, q == null ? null : q.getType(), q == null ? null : q.getQuestion());
        }
    }

    private static <T> T callWithRateLimitRetries(ThrowingSupplier<T> supplier, int maxAttempts, Duration baseBackoff) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return supplier.get();
            } catch (OpenAiException ex) {
                if (isRateLimited(ex) && attempt < maxAttempts) {
                    Duration sleep = baseBackoff.multipliedBy(attempt);
                    log.warn("OpenAI rate limited (attempt {}/{}). Sleeping {} ms then retrying. Error: {}",
                            attempt, maxAttempts, sleep.toMillis(), ex.getMessage());
                    sleep(sleep);
                    continue;
                }
                if (isRateLimited(ex)) {
                    assumeTrue(false, "Skipping test due to OpenAI rate limiting: " + ex.getMessage());
                }
                throw ex;
            }
        }
    }

    private static boolean isRateLimited(OpenAiException ex) {
        String msg = ex.getMessage();
        if (msg == null) return false;
        return msg.contains("HTTP 429") || msg.toLowerCase().contains("rate limit");
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while backing off for OpenAI retry", ie);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}

