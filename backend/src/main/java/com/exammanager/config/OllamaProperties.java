package com.exammanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ollama")
@Getter
@Setter
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String model = "gemma3";
    private int timeout = 120;
    private boolean enabled = true;
}
