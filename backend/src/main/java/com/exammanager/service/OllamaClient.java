package com.exammanager.service;

import com.exammanager.config.OllamaProperties;
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
public class OllamaClient implements LlmClient {

    private final OllamaProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(OllamaProperties properties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(properties.getTimeout()))
                .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    properties.getBaseUrl() + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Ollama is not available: {}", e.getMessage());
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
        // ChatMessage DTO를 Ollama API가 받는 Map 형태로 변환
        List<Map<String, String>> messageList = messages.stream()
                .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                .collect(Collectors.toList());
        return executeChat(messageList);
    }

    /**
     * Ollama /api/chat 엔드포인트에 messages를 전송하고 JSON 응답을 파싱한다.
     * 기존 단일턴과 멀티턴 양쪽에서 공통으로 사용하는 핵심 실행 메서드.
     */
    private JsonNode executeChat(List<Map<String, String>> messages) {
        String url = properties.getBaseUrl() + "/api/chat";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages);
        body.put("format", "json");
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.1));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isEmpty()) {
                log.error("Ollama returned unexpected response: status={}, body={}", response.getStatusCode(), response.getBody());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("Ollama chat request failed: {}", e.getMessage());
            return null;
        }
    }
}
