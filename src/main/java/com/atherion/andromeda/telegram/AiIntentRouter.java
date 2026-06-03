package com.atherion.andromeda.telegram;

import com.atherion.andromeda.services.AiService;
import com.atherion.andromeda.services.RagService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiIntentRouter {

    private static final String BASE_SYSTEM_PROMPT = """
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
              /rag_query                      — open question about the project (stories, tasks, sprints, status, summaries, etc.)

            Rules:
            - Respond ONLY with a JSON object; no markdown, no explanation, no extra text.
            - If the message maps to a command: {"cmd": "/command_name", "args": "arguments_here"}
            - If args are empty, use: {"cmd": "/command_name", "args": ""}
            - When the user asks about user stories for a project (not a feature), use /projectstories <projectId>.
            - When the user asks about user stories for a feature, use /userstories <featureId>.
            - When the user asks an open question about the project (e.g. "what tasks are pending?", "summarize the sprint", "which stories are in progress?"), use /rag_query with args "".
            - If you cannot map the message to any command: {"cmd": "none", "args": ""}
            - Always use numeric IDs in args. Use the known entities and active context listed below to resolve names to IDs.
            - If the user refers to "the project", "this project", "it", or omits an entity ID and one is active, use the active entity's ID.
            """;

    // Commands that accept a projectId as the sole arg
    private static final Set<String> PROJECT_ID_CMDS = Set.of(
            "/tasks", "/capabilities", "/projectstories",
            "/members", "/sprints", "/sprinttasks", "/suggest", "/analyze"
    );

    private final AiService                  aiService;
    private final RagService                 ragService;
    private final BotCommandHandler          commandHandler;
    private final ConversationSessionManager sessionManager;
    private final EntityResolver             entityResolver;

    /**
     * Route a natural language message to a bot command and return the result.
     * Uses conversation history for multi-turn context (Phase 3).
     * Returns null if no intent is found or AI is disabled.
     */
    public String route(String naturalText, Long telegramUserId) {
        if (!aiService.isEnabled()) return null;

        ConversationSession session = sessionManager.getOrCreate(telegramUserId);
        String systemPrompt = buildSystemPrompt(session);

        List<Map<String, String>> historyMaps = session.getHistory().stream()
                .map(msg -> Map.of("role", msg.role(), "content", msg.content()))
                .toList();

        JsonNode json = aiService.chatJsonWithHistory(systemPrompt, historyMaps, naturalText);
        if (json == null) {
            log.warn("AI returned no parseable intent for: {}", naturalText);
            return null;
        }

        String cmd  = json.path("cmd").asText("none").trim();
        String args = json.path("args").asText("").trim();

        if ("none".equals(cmd) || cmd.isBlank()) return null;

        // RAG path: open question answered via vector search + AI
        if ("rag_query".equals(cmd) || "/rag_query".equals(cmd)) {
            session.addToHistory("user", naturalText);
            String ragAnswer = ragService.query(naturalText, session.getActiveProjectId());
            if (ragAnswer != null) {
                session.addToHistory("assistant", truncate(ragAnswer));
                sessionManager.persistHistory(telegramUserId);
            }
            return ragAnswer;
        }

        // Resolve empty or named args using session context and entity resolver
        args = resolveArgs(cmd, args, session);

        String fullCommand = args.isBlank() ? cmd : cmd + " " + args;
        log.debug("NL '{}' → '{}'", naturalText, fullCommand);

        // Save this exchange to history and persist
        session.addToHistory("user", naturalText);
        session.addToHistory("assistant", truncate(fullCommand));
        sessionManager.persistHistory(telegramUserId);

        return commandHandler.handle(fullCommand, telegramUserId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private String buildSystemPrompt(ConversationSession session) {
        String contextSummary  = session.buildContextSummary();
        String projectList     = entityResolver.buildProjectList();
        String capabilityList  = entityResolver.buildCapabilityList(session.getActiveProjectId());
        String featureList     = entityResolver.buildFeatureList(session.getActiveCapabilityId());
        String userStoryList   = entityResolver.buildUserStoryList(session.getActiveFeatureId());

        boolean hasExtras = !contextSummary.isBlank() || !projectList.isBlank()
                || !capabilityList.isBlank() || !featureList.isBlank() || !userStoryList.isBlank();
        if (!hasExtras) return BASE_SYSTEM_PROMPT;

        StringBuilder sb = new StringBuilder(BASE_SYSTEM_PROMPT);
        if (!projectList.isBlank())    sb.append("\n").append(projectList);
        if (!capabilityList.isBlank()) sb.append("\n").append(capabilityList);
        if (!featureList.isBlank())    sb.append("\n").append(featureList);
        if (!userStoryList.isBlank())  sb.append("\n").append(userStoryList);
        if (!contextSummary.isBlank()) sb.append("\n\nUser's active context:\n").append(contextSummary);
        return sb.toString();
    }

    /**
     * Resolves args that are either empty (apply session fallback) or a name
     * (resolve to ID via EntityResolver, then fall back to session if unmatched).
     */
    private String resolveArgs(String cmd, String args, ConversationSession session) {
        // Empty args → apply session context as fallback
        if (args.isEmpty()) {
            return applySessionFallback(cmd, session, args);
        }
        // Numeric args → nothing to resolve
        if (isNumeric(args)) {
            return args;
        }
        // Named args → try name resolution, then session fallback
        return resolveByNameOrSession(cmd, args, session);
    }

    /** Uses session context to fill in a missing ID for the given command. */
    private String applySessionFallback(String cmd, ConversationSession session, String defaultArgs) {
        if (PROJECT_ID_CMDS.contains(cmd) && session.hasActiveProject())
            return session.getActiveProjectId().toString();
        if ("/features".equals(cmd) && session.hasActiveCapability())
            return session.getActiveCapabilityId().toString();
        if ("/userstories".equals(cmd) && session.hasActiveFeature())
            return session.getActiveFeatureId().toString();
        if ("/fix".equals(cmd) && session.hasActiveTask())
            return session.getActiveTaskId().toString();
        if ("/userstory".equals(cmd) && session.hasActiveUserStory())
            return session.getActiveUserStoryId().toString();
        return defaultArgs;
    }

    /** Tries to resolve a name to an ID, then falls back to the session. */
    private String resolveByNameOrSession(String cmd, String name, ConversationSession session) {
        if (PROJECT_ID_CMDS.contains(cmd) || "/project".equals(cmd)) {
            return entityResolver.resolveProjectByName(name)
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveProject()
                            ? session.getActiveProjectId().toString() : name);
        }
        if ("/features".equals(cmd)) {
            return entityResolver.resolveCapabilityByName(name, session.getActiveProjectId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveCapability()
                            ? session.getActiveCapabilityId().toString() : name);
        }
        if ("/userstories".equals(cmd)) {
            return entityResolver.resolveFeatureByName(name, session.getActiveCapabilityId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveFeature()
                            ? session.getActiveFeatureId().toString() : name);
        }
        if ("/capability".equals(cmd)) {
            return entityResolver.resolveCapabilityByName(name, session.getActiveProjectId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveCapability()
                            ? session.getActiveCapabilityId().toString() : name);
        }
        if ("/feature".equals(cmd)) {
            return entityResolver.resolveFeatureByName(name, session.getActiveCapabilityId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveFeature()
                            ? session.getActiveFeatureId().toString() : name);
        }
        if ("/userstory".equals(cmd)) {
            return entityResolver.resolveUserStoryByTitle(name, session.getActiveFeatureId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveUserStory()
                            ? session.getActiveUserStoryId().toString() : name);
        }
        if ("/fix".equals(cmd) || "/task".equals(cmd)) {
            return entityResolver.resolveTaskByTitle(name, session.getActiveProjectId())
                    .map(Object::toString)
                    .orElseGet(() -> session.hasActiveTask()
                            ? session.getActiveTaskId().toString() : name);
        }
        return name;
    }

    private static boolean isNumeric(String s) {
        return s.matches("\\d+");
    }

    private static String truncate(String text) {
        if (text == null) return null;
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }
}
