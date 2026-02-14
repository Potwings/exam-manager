package com.exammanager.service;

import com.exammanager.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class OllamaClient {

    private final OllamaProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(OllamaProperties properties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
    }

    public boolean isAvailable() {
        if (!properties.isEnabled()) return false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    properties.getBaseUrl() + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Ollama is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send a chat request to Ollama and return parsed JSON response.
     */
    public JsonNode chat(String systemPrompt, String userPrompt) {
        String url = properties.getBaseUrl() + "/api/chat";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("format", "json");
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.1));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("Ollama chat request failed: {}", e.getMessage());
            return null;
        }
    }
}
