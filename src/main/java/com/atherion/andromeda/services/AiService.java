package com.atherion.andromeda.services;

import com.atherion.andromeda.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiService {

    private final AiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient client;

    public AiService(AiProperties props) {
        this.props = props;
        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public String getModel() {
        return props.getModel();
    }

    /**
     * Call the AI with a system prompt + user message and return the stripped text response.
     * Removes <think>...</think> blocks produced by thinking models (e.g. Qwen3).
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!isEnabled()) return null;
        try {
            Map<String, Object> body = buildRequest(systemPrompt, userMessage);
            String raw = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractContent(raw);
        } catch (Exception e) {
            log.error("AI chat call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Like chat(), but extracts and parses the first JSON object found in the response.
     * Use when asking the AI to return structured JSON (e.g. intent routing).
     */
    public JsonNode chatJson(String systemPrompt, String userMessage) {
        String content = chat(systemPrompt, userMessage);
        if (content == null) return null;
        try {
            // Strip markdown code fences if the model wrapped the JSON
            content = content.replaceAll("(?s)```[a-z]*\\s*", "").replaceAll("```", "").trim();
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start < 0 || end < 0 || end < start) {
                log.warn("No JSON object found in AI response: {}", content);
                return null;
            }
            return objectMapper.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            log.warn("Could not parse AI JSON response: {}", content);
            return null;
        }
    }

    /**
     * Send a test message and return round-trip latency in ms, or -1 on failure.
     */
    public long ping() {
        if (!isEnabled()) return -1L;
        long start = System.currentTimeMillis();
        String result = chat("You are a test assistant.", "Reply with exactly one word: pong");
        return result != null ? System.currentTimeMillis() - start : -1L;
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildRequest(String systemPrompt, String userMessage) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", props.getModel());
        req.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        return req;
    }

    /**
     * Parse the OpenAI-compatible response JSON and return the cleaned content text.
     * Strips <think>...</think> scratchpad sections emitted by Qwen3 thinking mode.
     */
    private String extractContent(String rawJson) {
        try {
            JsonNode root    = objectMapper.readTree(rawJson);
            JsonNode message = root.path("choices").get(0).path("message");
            String content   = message.path("content").asText("");

            // Remove <think>...</think> blocks (thinking model scratchpad)
            content = content.replaceAll("(?s)<think>.*?</think>", "").trim();

            return content.isBlank() ? null : content;
        } catch (Exception e) {
            log.error("Failed to parse AI API response: {}", e.getMessage());
            return null;
        }
    }
}
