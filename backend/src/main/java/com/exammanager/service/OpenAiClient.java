package com.exammanager.service;

import com.exammanager.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
public class OpenAiClient implements LlmClient {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiClient(OpenAiProperties properties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
    }

    @Override
    public boolean isAvailable() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(properties.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    BASE_URL + "/models", HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("OpenAI is not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public JsonNode chat(String systemPrompt, String userPrompt) {
        String url = BASE_URL + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", 0.1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isEmpty()) {
                log.error("OpenAI returned unexpected response: status={}, body={}", response.getStatusCode(), response.getBody());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                log.error("OpenAI returned no choices: {}", response.getBody());
                return null;
            }
            String content = choices.path(0).path("message").path("content").asText();
            if (content.isBlank()) {
                log.error("OpenAI returned empty content");
                return null;
            }
            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("OpenAI chat request failed: {}", e.getMessage());
            return null;
        }
    }
}
