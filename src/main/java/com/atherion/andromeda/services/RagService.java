package com.atherion.andromeda.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are a project management assistant for the Andromeda system.
            Answer questions using ONLY the information from the provided context.
            If the information is not in the context, state it clearly.
            Respond in the same language the user writes in. Be concise and structured.
            """;

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final AiService aiService;

    public String query(String question, Long projectId) {
        float[] vector = embeddingService.embed(question);
        if (vector == null) return null;

        List<String> chunks = vectorStoreService.search(vector, 5, projectId);
        if (chunks.isEmpty()) {
            log.warn("RAG: no relevant chunks found for: {}", question);
            return null;
        }

        String context = String.join("\n\n---\n\n", chunks);
        String systemPrompt = SYSTEM_PROMPT + "\n\nProject context:\n" + context;
        return aiService.chat(systemPrompt, question);
    }
}
