package com.exammanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAiProperties {
    private String apiKey = "";
    private String model = "gpt-4o";
    private int timeout = 120;
}
