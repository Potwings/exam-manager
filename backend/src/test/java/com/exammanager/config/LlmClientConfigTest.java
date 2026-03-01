package com.exammanager.config;

import com.exammanager.service.LlmClient;
import com.exammanager.service.OllamaClient;
import com.exammanager.service.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmClientConfigTest {

    private final LlmClientConfig config = new LlmClientConfig();
    private final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private LlmProperties llmProperties(String provider) {
        LlmProperties props = new LlmProperties();
        props.setProvider(provider);
        return props;
    }

    @Test
    void provider가_ollama이면_OllamaClient를_생성한다() {
        LlmClient client = config.llmClient(
                llmProperties("ollama"),
                new OllamaProperties(),
                new OpenAiProperties(),
                restTemplateBuilder,
                objectMapper
        );

        assertThat(client).isInstanceOf(OllamaClient.class);
    }

    @Test
    void provider가_openai이면_OpenAiClient를_생성한다() {
        LlmClient client = config.llmClient(
                llmProperties("openai"),
                new OllamaProperties(),
                new OpenAiProperties(),
                restTemplateBuilder,
                objectMapper
        );

        assertThat(client).isInstanceOf(OpenAiClient.class);
    }

    @Test
    void provider가_none이면_비활성_클라이언트를_생성한다() {
        LlmClient client = config.llmClient(
                llmProperties("none"),
                new OllamaProperties(),
                new OpenAiProperties(),
                restTemplateBuilder,
                objectMapper
        );

        assertThat(client).isNotNull();
        assertThat(client.isAvailable()).isFalse();
        assertThat(client.chat("system", "user")).isNull();
    }

    @Test
    void provider가_잘못된_값이면_예외를_던진다() {
        assertThatThrownBy(() -> config.llmClient(
                llmProperties("unknown"),
                new OllamaProperties(),
                new OpenAiProperties(),
                restTemplateBuilder,
                objectMapper
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown LLM provider: unknown");
    }
}
