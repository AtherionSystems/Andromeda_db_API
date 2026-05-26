package com.atherion.andromeda;

import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.repositories.ConversationSessionRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.services.CapabilityService;
import com.atherion.andromeda.services.FeatureService;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.TasksService;
import com.atherion.andromeda.services.UserStoryService;
import com.atherion.andromeda.telegram.ConversationSession;
import com.atherion.andromeda.telegram.ConversationSessionManager;
import com.atherion.andromeda.telegram.EntityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryTest {

    @Mock private ConversationSessionRepository sessionRepo;
    @Mock private UserRepository                userRepo;

    // ── ConversationSession ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ConversationSession — context state")
    class SessionStateTests {

        private ConversationSession session;

        @BeforeEach
        void setUp() {
            session = new ConversationSession(42L);
        }

        @Test
        @DisplayName("New session has no active context")
        void newSession_noContext() {
            assertFalse(session.hasActiveProject());
            assertFalse(session.hasActiveCapability());
            assertFalse(session.hasActiveFeature());
            assertFalse(session.hasActiveTask());
            assertTrue(session.buildContextSummary().isBlank());
        }

        @Test
        @DisplayName("Setting project sets activeProjectId and name")
        void setProject_storesIdAndName() {
            session.setActiveProject(134L, "Andromeda");

            assertTrue(session.hasActiveProject());
            assertEquals(134L, session.getActiveProjectId());
            assertEquals("Andromeda", session.getActiveProjectName());
        }

        @Test
        @DisplayName("Changing project clears capability, feature and task contexts")
        void changeProject_clearsChildContexts() {
            session.setActiveProject(1L, "ProjectA");
            session.setActiveCapability(10L, "Cap1");
            session.setActiveFeature(20L, "Feat1");
            session.setActiveTask(30L, "Task1");

            // switch to a different project
            session.setActiveProject(2L, "ProjectB");

            assertFalse(session.hasActiveCapability(), "capability should be cleared");
            assertFalse(session.hasActiveFeature(),    "feature should be cleared");
            assertFalse(session.hasActiveTask(),       "task should be cleared");
        }

        @Test
        @DisplayName("Changing capability clears feature context but keeps project")
        void changeCapability_clearsFeatureKeepsProject() {
            session.setActiveProject(1L, "ProjectA");
            session.setActiveCapability(10L, "Cap1");
            session.setActiveFeature(20L, "Feat1");

            session.setActiveCapability(11L, "Cap2");

            assertTrue(session.hasActiveProject(),     "project should be kept");
            assertFalse(session.hasActiveFeature(),    "feature should be cleared");
            assertEquals(11L, session.getActiveCapabilityId());
        }

        @Test
        @DisplayName("buildContextSummary includes all active entities")
        void contextSummary_includesAllActiveEntities() {
            session.setActiveProject(134L, "Andromeda");
            session.setActiveCapability(5L, "Authentication");
            session.setActiveFeature(8L, "Login Flow");
            session.setActiveTask(22L, "Fix redirect bug");

            String summary = session.buildContextSummary();

            assertTrue(summary.contains("Andromeda"));
            assertTrue(summary.contains("134"));
            assertTrue(summary.contains("Authentication"));
            assertTrue(summary.contains("Login Flow"));
            assertTrue(summary.contains("Fix redirect bug"));
        }

        @Test
        @DisplayName("buildContextSummary only shows entities that are set")
        void contextSummary_partialContext() {
            session.setActiveProject(134L, "Andromeda");

            String summary = session.buildContextSummary();

            assertTrue(summary.contains("Andromeda"));
            assertFalse(summary.contains("capability"));
            assertFalse(summary.contains("feature"));
            assertFalse(summary.contains("task"));
        }
    }

    // ── ConversationSessionManager ─────────────────────────────────────────────

    @Nested
    @DisplayName("ConversationSessionManager — session lifecycle")
    class SessionManagerTests {

        private ConversationSessionManager manager;

        @BeforeEach
        void setUp() {
            manager = new ConversationSessionManager(sessionRepo, userRepo);
        }

        @Test
        @DisplayName("getOrCreate creates a new session on first call")
        void getOrCreate_createsNewSession() {
            ConversationSession session = manager.getOrCreate(1L);
            assertNotNull(session);
            assertFalse(session.hasActiveProject());
        }

        @Test
        @DisplayName("getOrCreate returns the same session instance on subsequent calls")
        void getOrCreate_returnsSameInstance() {
            ConversationSession first  = manager.getOrCreate(1L);
            ConversationSession second = manager.getOrCreate(1L);
            assertSame(first, second);
        }

        @Test
        @DisplayName("Different user IDs get independent sessions")
        void differentUsers_independentSessions() {
            manager.setActiveProject(1L, 100L, "ProjectAlpha");
            manager.setActiveProject(2L, 200L, "ProjectBeta");

            assertEquals(100L, manager.getOrCreate(1L).getActiveProjectId());
            assertEquals(200L, manager.getOrCreate(2L).getActiveProjectId());
        }

        @Test
        @DisplayName("setActiveProject updates session through manager")
        void setActiveProject_updatesSession() {
            manager.setActiveProject(1L, 134L, "Andromeda");

            ConversationSession session = manager.getOrCreate(1L);
            assertEquals(134L, session.getActiveProjectId());
            assertEquals("Andromeda", session.getActiveProjectName());
        }

        @Test
        @DisplayName("Null userId is a no-op and does not throw")
        void nullUserId_isNoop() {
            assertDoesNotThrow(() -> manager.setActiveProject(null, 1L, "Project"));
        }
    }

    // ── EntityResolver ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EntityResolver — name to ID resolution")
    class EntityResolverTests {

        @Mock private ProjectService    projectService;
        @Mock private CapabilityService capabilityService;
        @Mock private FeatureService    featureService;
        @Mock private TasksService      tasksService;
        @Mock private UserStoryService  userStoryService;

        private EntityResolver resolver;

        @BeforeEach
        void setUp() {
            resolver = new EntityResolver(projectService, capabilityService, featureService, tasksService, userStoryService);
        }

        @Test
        @DisplayName("resolveProjectByName — exact match returns ID")
        void resolveProject_exactMatch() {
            Project p = project(134L, "Andromeda");
            when(projectService.findAll()).thenReturn(List.of(p));

            Optional<Long> result = resolver.resolveProjectByName("Andromeda");

            assertTrue(result.isPresent());
            assertEquals(134L, result.get());
        }

        @Test
        @DisplayName("resolveProjectByName — case-insensitive match returns ID")
        void resolveProject_caseInsensitive() {
            Project p = project(134L, "Andromeda");
            when(projectService.findAll()).thenReturn(List.of(p));

            Optional<Long> result = resolver.resolveProjectByName("andromeda");

            assertTrue(result.isPresent());
            assertEquals(134L, result.get());
        }

        @Test
        @DisplayName("resolveProjectByName — partial substring match returns ID")
        void resolveProject_substringMatch() {
            Project p = project(134L, "Andromeda Project");
            when(projectService.findAll()).thenReturn(List.of(p));

            Optional<Long> result = resolver.resolveProjectByName("andromeda");

            assertTrue(result.isPresent());
            assertEquals(134L, result.get());
        }

        @Test
        @DisplayName("resolveProjectByName — no match returns empty")
        void resolveProject_noMatch() {
            Project p = project(1L, "OtherProject");
            when(projectService.findAll()).thenReturn(List.of(p));

            Optional<Long> result = resolver.resolveProjectByName("Andromeda");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("resolveCapabilityByName — scoped to projectId")
        void resolveCapability_scopedToProject() {
            Capability cap = capability(5L, "Authentication");
            when(capabilityService.findByProjectId(134L)).thenReturn(List.of(cap));

            Optional<Long> result = resolver.resolveCapabilityByName("authentication", 134L);

            assertTrue(result.isPresent());
            assertEquals(5L, result.get());
        }

        @Test
        @DisplayName("resolveTaskByTitle — scoped to projectId")
        void resolveTask_scopedToProject() {
            Tasks task = task(22L, "Fix redirect bug");
            when(tasksService.findByProjectId(134L)).thenReturn(List.of(task));

            Optional<Long> result = resolver.resolveTaskByTitle("fix redirect", 134L);

            assertTrue(result.isPresent());
            assertEquals(22L, result.get());
        }

        @Test
        @DisplayName("buildProjectList — formats project names and IDs")
        void buildProjectList_formatsCorrectly() {
            when(projectService.findAll()).thenReturn(List.of(
                    project(1L, "Alpha"),
                    project(2L, "Beta")
            ));

            String list = resolver.buildProjectList();

            assertTrue(list.contains("[1] Alpha"));
            assertTrue(list.contains("[2] Beta"));
        }

        @Test
        @DisplayName("buildProjectList — returns empty string when no projects")
        void buildProjectList_emptyWhenNoProjects() {
            when(projectService.findAll()).thenReturn(List.of());

            String list = resolver.buildProjectList();

            assertTrue(list.isBlank());
        }

        // ── test data helpers ─────────────────────────────────────────────────

        private Project project(Long id, String name) {
            Project p = new Project();
            p.setId(id);
            p.setName(name);
            p.setStatus("active");
            return p;
        }

        private Capability capability(Long id, String name) {
            Capability c = new Capability();
            c.setId(id);
            c.setName(name);
            return c;
        }

        private Tasks task(Long id, String title) {
            Tasks t = new Tasks();
            t.setId(id);
            t.setTitle(title);
            return t;
        }
    }
}
