package com.atherion.andromeda.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.ai")
@Getter
@Setter
public class AiProperties {
    private boolean enabled = true;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
    private String apiKey;
    private String model = "gemini-flash-latest";
    private String embeddingModel = "gemini-embedding-2";
}
