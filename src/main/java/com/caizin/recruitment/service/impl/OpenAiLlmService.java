package com.caizin.recruitment.service.impl;

import com.caizin.recruitment.config.OpenAiProperties;
import com.caizin.recruitment.dto.JobDto;
import com.caizin.recruitment.dto.OpenAiRequestDto;
import com.caizin.recruitment.dto.OpenAiResponseDto;
import com.caizin.recruitment.dto.ScreeningQuestionDto;
import com.caizin.recruitment.exception.OpenAiException;
import com.caizin.recruitment.integration.openai.OpenAiClient;
import com.caizin.recruitment.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Production LLM service: builds prompt, calls OpenAI, parses response into screening questions.
 * Not wired into orchestration yet; provided for future use.
 */
@Service
public class OpenAiLlmService implements LlmService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmService.class);

    private final OpenAiClient openAiClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiLlmService(OpenAiClient openAiClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.openAiClient = Objects.requireNonNull(openAiClient, "openAiClient");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public List<ScreeningQuestionDto> generateQuestions(JobDto job) {
        validateJob(job);

        String prompt = buildPrompt(job);
        String model = requireModel();
        Double temperature = properties.getTemperature() != null ? properties.getTemperature() : 0.2;

        OpenAiRequestDto request = new OpenAiRequestDto(
                model,
                List.of(new OpenAiRequestDto.Message("user", prompt)),
                temperature,
                new OpenAiRequestDto.ResponseFormat("json_object")
        );

        OpenAiResponseDto response = openAiClient.createChatCompletion(request);
        String content = extractContent(response);
        List<ScreeningQuestionDto> questions = parseQuestionsJson(content);

        validateQuestions(questions);
        log.info("Generated {} screening question(s) for jobId={}", questions.size(), job.getJobId());

        return questions;
    }

    private static void validateJob(JobDto job) {
        if (job == null) {
            throw new OpenAiException("Job must not be null");
        }
        boolean hasTitle = job.getTitle() != null && !job.getTitle().isBlank();
        boolean hasDescription = job.getDescription() != null && !job.getDescription().isBlank();
        if (!hasTitle && !hasDescription) {
            throw new OpenAiException("Job must have at least title or description");
        }
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

    private String requireModel() {
        String model = properties.getModel();
        if (model == null || model.isBlank()) {
            throw new OpenAiException("OpenAI model is not configured (openai.model)");
        }
        return model.trim();
    }

    private static String extractContent(OpenAiResponseDto response) {
        if (response == null) {
            throw new OpenAiException("OpenAI response is null");
        }
        List<OpenAiResponseDto.Choice> choices = response.getChoices();
        if (choices == null || choices.isEmpty()) {
            throw new OpenAiException("OpenAI response missing choices");
        }
        OpenAiResponseDto.Choice first = choices.get(0);
        if (first == null || first.getMessage() == null) {
            throw new OpenAiException("OpenAI response missing message");
        }
        String content = first.getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new OpenAiException("OpenAI returned empty message content");
        }
        return content.trim();
    }

    private List<ScreeningQuestionDto> parseQuestionsJson(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode questionsNode = root == null ? null : root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) {
                throw new OpenAiException("OpenAI response JSON missing 'questions' array");
            }

            List<ScreeningQuestionDto> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                String type = text(node, "type");
                String question = text(node, "question");
                if (question == null || question.isBlank()) {
                    continue;
                }
                questions.add(new ScreeningQuestionDto(type, question));
            }

            return questions;
        } catch (OpenAiException e) {
            throw e;
        } catch (Exception e) {
            throw new OpenAiException("Failed to parse OpenAI response as JSON", e);
        }
    }

    private static void validateQuestions(List<ScreeningQuestionDto> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new OpenAiException("OpenAI returned no valid questions");
        }
        for (int i = 0; i < questions.size(); i++) {
            ScreeningQuestionDto q = questions.get(i);
            if (q == null || q.getQuestion() == null || q.getQuestion().isBlank()) {
                throw new OpenAiException("Question at index " + i + " has blank or null text");
            }
        }
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
