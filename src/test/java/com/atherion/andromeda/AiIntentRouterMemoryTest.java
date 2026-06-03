package com.atherion.andromeda;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.repositories.ConversationSessionRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.services.AiService;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.RagService;
import com.atherion.andromeda.telegram.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that AiIntentRouter correctly uses session context and entity
 * name resolution — without a real AI or Telegram connection.
 *
 * Strategy: mock AiService to return a pre-set JSON intent, then assert
 * which command BotCommandHandler receives.
 */
@ExtendWith(MockitoExtension.class)
class AiIntentRouterMemoryTest {

    @Mock private AiService                      aiService;
    @Mock private RagService                     ragService;
    @Mock private BotCommandHandler              commandHandler;
    @Mock private ProjectService                 projectService;
    @Mock private ConversationSessionRepository  sessionRepo;
    @Mock private UserRepository                 userRepo;

    private ConversationSessionManager sessionManager;
    private EntityResolver             entityResolver;
    private AiIntentRouter             router;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        sessionManager = new ConversationSessionManager(sessionRepo, userRepo);
        entityResolver = new EntityResolver(
                projectService,
                mock(com.atherion.andromeda.services.CapabilityService.class),
                mock(com.atherion.andromeda.services.FeatureService.class),
                mock(com.atherion.andromeda.services.TasksService.class),
                mock(com.atherion.andromeda.services.UserStoryService.class)
        );
        router = new AiIntentRouter(aiService, ragService, commandHandler, sessionManager, entityResolver);
        when(aiService.isEnabled()).thenReturn(true);
    }

    // ── Phase 1: Session context fallback ─────────────────────────────────────

    @Nested
    @DisplayName("Phase 1 — Session context fallback")
    class SessionFallbackTests {

        @Test
        @DisplayName("Empty args + active project → uses project ID automatically")
        void emptyArgs_withActiveProject_usesSessionId() throws Exception {
            // Simulate user previously viewed project 134
            sessionManager.setActiveProject(USER_ID, 134L, "Andromeda");

            // AI returns empty args (user said "show me tasks" without specifying project)
            aiIntent("/tasks", "");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Tasks list...");

            router.route("show me tasks", USER_ID);

            verify(commandHandler).handle("/tasks 134", USER_ID);
        }

        @Test
        @DisplayName("No active context + empty args → passes command through as-is")
        void emptyArgs_noContext_passesThrough() throws Exception {
            aiIntent("/tasks", "");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn(null);

            router.route("show me tasks", USER_ID);

            // No session fallback available — command passed without args
            verify(commandHandler).handle("/tasks", USER_ID);
        }

        @Test
        @DisplayName("Numeric args → passed through unchanged regardless of session")
        void numericArgs_passedThroughUnchanged() throws Exception {
            // Even if there is an active project, numeric args are used as-is
            sessionManager.setActiveProject(USER_ID, 134L, "Andromeda");

            aiIntent("/tasks", "999");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Tasks list...");

            router.route("show tasks in 999", USER_ID);

            verify(commandHandler).handle("/tasks 999", USER_ID);
        }

        @Test
        @DisplayName("Viewing a project stores it in session for subsequent queries")
        void viewProject_thenQueryTasks_usesStoredProjectId() throws Exception {
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("dummy");

            // Simulate the session update that BotCommandHandler would do after /project 134
            sessionManager.setActiveProject(USER_ID, 134L, "Andromeda");

            // AI returns /tasks with empty args (user says "muéstrame las tareas")
            aiIntent("/tasks", "");
            router.route("muéstrame las tareas", USER_ID);

            verify(commandHandler).handle("/tasks 134", USER_ID);
        }

        @Test
        @DisplayName("Empty args for /features uses active capability from session")
        void emptyArgs_withActiveCapability_usesCapabilityId() throws Exception {
            sessionManager.setActiveProject(USER_ID, 134L, "Andromeda");
            sessionManager.setActiveCapability(USER_ID, 5L, "Authentication");

            aiIntent("/features", "");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Features...");

            router.route("show features", USER_ID);

            verify(commandHandler).handle("/features 5", USER_ID);
        }
    }

    // ── Phase 2: Entity name resolution ───────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — Entity name resolution")
    class NameResolutionTests {

        @Test
        @DisplayName("Named project arg → resolved to ID via EntityResolver")
        void namedProjectArg_resolvedToId() throws Exception {
            when(projectService.findAll()).thenReturn(List.of(project(134L, "Andromeda")));
            aiIntent("/tasks", "Andromeda");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Tasks...");

            router.route("tareas del proyecto andromeda", USER_ID);

            verify(commandHandler).handle("/tasks 134", USER_ID);
        }

        @Test
        @DisplayName("Named arg case-insensitive — lowercase still resolves")
        void namedArg_caseInsensitive() throws Exception {
            when(projectService.findAll()).thenReturn(List.of(project(134L, "Andromeda")));
            aiIntent("/tasks", "andromeda");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Tasks...");

            router.route("tasks of andromeda", USER_ID);

            verify(commandHandler).handle("/tasks 134", USER_ID);
        }

        @Test
        @DisplayName("Named arg with no match + active session → falls back to session ID")
        void namedArg_noMatch_fallsBackToSession() throws Exception {
            when(projectService.findAll()).thenReturn(List.of(project(1L, "OtherProject")));
            sessionManager.setActiveProject(USER_ID, 134L, "Andromeda");

            // AI thinks the project is called "UnknownProject", EntityResolver won't find it
            aiIntent("/tasks", "UnknownProject");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn("Tasks...");

            router.route("tasks of unknownproject", USER_ID);

            // Fallback: use the active session project
            verify(commandHandler).handle("/tasks 134", USER_ID);
        }

        @Test
        @DisplayName("Named arg with no match and no session → passes name through")
        void namedArg_noMatchNoSession_passesNameThrough() throws Exception {
            when(projectService.findAll()).thenReturn(List.of());
            aiIntent("/tasks", "UnknownProject");
            when(commandHandler.handle(anyString(), anyLong())).thenReturn(null);

            router.route("tasks of unknownproject", USER_ID);

            // Name is passed through unchanged (BotCommandHandler will return "not found")
            verify(commandHandler).handle("/tasks UnknownProject", USER_ID);
        }
    }

    // ── AI disabled ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns null immediately when AI is disabled")
    void aiDisabled_returnsNull() {
        when(aiService.isEnabled()).thenReturn(false);

        String result = router.route("show me tasks", USER_ID);

        assertNull(result);
        verifyNoInteractions(commandHandler);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void aiIntent(String cmd, String args) throws Exception {
        JsonNode json = mapper.readTree("{\"cmd\": \"" + cmd + "\", \"args\": \"" + args + "\"}");
        when(aiService.chatJsonWithHistory(anyString(), any(), anyString())).thenReturn(json);
    }

    private Project project(Long id, String name) {
        Project p = new Project();
        p.setId(id);
        p.setName(name);
        p.setStatus("active");
        return p;
    }

    private static void assertNull(Object obj) {
        if (obj != null) throw new AssertionError("Expected null but was: " + obj);
    }
}
