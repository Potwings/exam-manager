package com.exammanager.service;

import com.exammanager.config.OpenAiProperties;
import com.exammanager.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
        return executeChat(messages);
    }

    @Override
    public JsonNode chat(List<ChatMessage> messages) {
        // ChatMessage DTO를 OpenAI API가 받는 Map 형태로 변환
        List<Map<String, String>> messageList = messages.stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());
        return executeChat(messageList);
    }

    /**
     * OpenAI /v1/chat/completions 엔드포인트에 messages를 전송하고 JSON 응답을 파싱한다.
     * 기존 단일턴과 멀티턴 양쪽에서 공통으로 사용하는 핵심 실행 메서드.
     */
    private JsonNode executeChat(List<Map<String, String>> messages) {
        String url = BASE_URL + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages);
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
