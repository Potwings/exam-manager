package com.exammanager.config;

import com.exammanager.service.LlmClient;
import com.exammanager.service.OllamaClient;
import com.exammanager.service.OpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LlmClientConfig {

    @Bean
    public LlmClient llmClient(LlmProperties llmProperties,
                                OllamaProperties ollamaProperties,
                                OpenAiProperties openAiProperties,
                                RestTemplateBuilder restTemplateBuilder,
                                ObjectMapper objectMapper) {
        String provider = llmProperties.getProvider();
        log.info("LLM provider: {}", provider);

        return switch (provider) {
            case "ollama" -> new OllamaClient(ollamaProperties, restTemplateBuilder, objectMapper);
            case "openai" -> new OpenAiClient(openAiProperties, restTemplateBuilder, objectMapper);
            case "none" -> new LlmClient() {
                @Override
                public boolean isAvailable() { return false; }
                @Override
                public JsonNode chat(String systemPrompt, String userPrompt) { return null; }
            };
            default -> throw new IllegalArgumentException("Unknown LLM provider: " + provider
                    + ". Supported: ollama, openai, none");
        };
    }
}
