package com.atherion.andromeda.services;

import com.atherion.andromeda.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingService {

    private final AiProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddingService(AiProperties props) {
        this.props = props;
    }

    public float[] embed(String text) {
        if (!props.isEnabled() || text == null || text.isBlank()) return null;
        try {
            // text-embedding-004 uses the native Gemini endpoint, not the OpenAI-compatible one
            String nativeBase = props.getBaseUrl()
                    .replace("/openai", "")
                    .replace("v1beta", "v1");
            String url = nativeBase + "/models/" + props.getEmbeddingModel() + ":embedContent";

            Map<String, Object> body = Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            );

            String raw = RestClient.create()
                    .post()
                    .uri(url)
                    .header("x-goog-api-key", props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseEmbedding(raw);
        } catch (Exception e) {
            log.error("Embedding call failed: {}", e.getMessage());
            return null;
        }
    }

    private float[] parseEmbedding(String raw) {
        try {
            JsonNode values = objectMapper.readTree(raw).path("embedding").path("values");
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = (float) values.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", e.getMessage());
            return null;
        }
    }
}
