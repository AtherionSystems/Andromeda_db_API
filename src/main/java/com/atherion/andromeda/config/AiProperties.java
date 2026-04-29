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
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String apiKey;
    private String model = "qwen/qwen3-32b";
}
