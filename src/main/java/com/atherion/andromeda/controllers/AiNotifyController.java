package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.NotifyRequest;
import com.atherion.andromeda.services.AiService;
import com.atherion.andromeda.telegram.AndromedaBot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiNotifyController {

    private final AiService aiService;
    private final AndromedaBot bot;

    /**
     * POST /api/ai/notify
     * Body: { "chatId": "...", "context": "description of the event to notify about" }
     *
     * Generates a smart, human-friendly Telegram notification via AI and sends it
     * to the given chat. Use this from CI/CD pipelines or monitoring systems.
     */
    @PostMapping("/notify")
    public ResponseEntity<String> notify(@RequestBody NotifyRequest req) {
        if (req.getChatId() == null || req.getChatId().isBlank())
            return ResponseEntity.badRequest().body("chatId is required.");
        if (req.getContext() == null || req.getContext().isBlank())
            return ResponseEntity.badRequest().body("context is required.");

        if (!aiService.isEnabled()) {
            bot.sendText(req.getChatId(), req.getContext());
            return ResponseEntity.ok("AI disabled — raw context forwarded.");
        }

        String systemPrompt = """
                You are a smart notification assistant for the Andromeda project management system.
                Given a context string describing a project event, write a concise Telegram notification.
                Keep it under 250 characters. Be clear about what happened and include any necessary action.
                Do not use markdown formatting. Plain text only.
                """;

        String message = aiService.chat(systemPrompt, req.getContext());
        if (message == null)
            return ResponseEntity.internalServerError().body("AI did not respond.");

        bot.sendText(req.getChatId(), message);
        return ResponseEntity.ok("Notification sent.");
    }

    /**
     * GET /api/ai/status
     * Returns the AI backend status and configured model.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        if (!aiService.isEnabled())
            return ResponseEntity.ok("AI disabled (agent.ai.enabled=false).");

        long latency = aiService.ping();
        if (latency < 0)
            return ResponseEntity.status(503).body("AI backend unreachable.");

        return ResponseEntity.ok("AI online | model: " + aiService.getModel() + " | latency: " + latency + " ms");
    }
}
