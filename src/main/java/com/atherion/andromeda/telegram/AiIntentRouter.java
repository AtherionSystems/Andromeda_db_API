package com.atherion.andromeda.telegram;

import com.atherion.andromeda.services.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiIntentRouter {

    private static final String SYSTEM_PROMPT = """
            You are a routing assistant for the Andromeda project management Telegram bot.
            Given a natural language message from a user, map it to the closest matching bot command.

            The Andromeda hierarchy is: Project → Capabilities → Features → User Stories → Tasks.

            Available commands:
              /projects                       — list all projects
              /project <id>                   — project details
              /capabilities <projectId>       — list capabilities in a project
              /capability <id>                — capability details
              /features <capabilityId>        — list features in a capability
              /feature <id>                   — feature details
              /projectstories <projectId>     — all user stories in a project
              /userstories <featureId>        — user stories in a specific feature
              /userstory <id>                 — user story details
              /tasks <projectId>              — list tasks in a project
              /task <id>                      — task details
              /members <projectId>            — list project members
              /sprints <projectId>            — list project sprints
              /sprinttasks <projectId>        — sprint task board
              /users                          — list all users
              /user <id>                      — user details
              /suggest <projectId>            — AI suggestions for a project
              /analyze <projectId>            — AI health analysis of a project
              /fix <taskId>                   — AI guidance to resolve a task
              /pingai                         — test AI backend connectivity

            Rules:
            - Respond ONLY with a JSON object; no markdown, no explanation, no extra text.
            - If the message maps to a command: {"cmd": "/command_name", "args": "arguments_here"}
            - If args are empty, use: {"cmd": "/command_name", "args": ""}
            - When the user asks about user stories for a project (not a feature), use /projectstories <projectId>.
            - When the user asks about user stories for a feature, use /userstories <featureId>.
            - If you cannot map the message to any command: {"cmd": "none", "args": ""}
            """;

    private final AiService aiService;
    private final BotCommandHandler commandHandler;

    /**
     * Route a natural language message to a bot command and return the result.
     * Returns null if no intent is found or AI is disabled.
     */
    public String route(String naturalText, Long telegramUserId) {
        if (!aiService.isEnabled()) return null;

        JsonNode json = aiService.chatJson(SYSTEM_PROMPT, naturalText);
        if (json == null) {
            log.warn("AI returned no parseable intent for: {}", naturalText);
            return null;
        }

        String cmd  = json.path("cmd").asText("none").trim();
        String args = json.path("args").asText("").trim();

        if ("none".equals(cmd) || cmd.isBlank()) return null;

        String fullCommand = args.isBlank() ? cmd : cmd + " " + args;
        log.debug("NL '{}' → '{}'", naturalText, fullCommand);
        return commandHandler.handle(fullCommand, telegramUserId);
    }
}
