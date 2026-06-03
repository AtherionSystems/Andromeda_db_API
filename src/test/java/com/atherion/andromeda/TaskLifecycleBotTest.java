package com.atherion.andromeda;

import com.atherion.andromeda.dto.SprintTaskRow;
import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import com.atherion.andromeda.telegram.BotCommandHandler;
import com.atherion.andromeda.telegram.ConversationSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Automated bot tests covering the full task lifecycle:
 *   1. Create the next task
 *   2. Cancel a task
 *   3. Assign a task to a team member
 *   4. Complete a task
 *   5. View a developer's tasks
 *   6. View a developer's KPIs
 *   7. Managers can view their team's tasks
 */
@ExtendWith(MockitoExtension.class)
class TaskLifecycleBotTest {

    // ── mocked services ───────────────────────────────────────────────────────

    @Mock private ProjectService                 projectService;
    @Mock private TasksService                   tasksService;
    @Mock private UserService                    userService;
    @Mock private ProjectMemberService           projectMemberService;
    @Mock private SprintService                  sprintService;
    @Mock private SprintStoryAssignmentService   sprintStoryAssignmentService;
    @Mock private TaskAssignmentService          taskAssignmentService;
    @Mock private CapabilityService              capabilityService;
    @Mock private FeatureService                 featureService;
    @Mock private UserStoryService               userStoryService;
    @Mock private BCryptPasswordEncoder          passwordEncoder;
    @Mock private AiService                      aiService;
    @Mock private ConversationSessionManager     sessionManager;

    private BotCommandHandler handler;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private static final long MANAGER_TG_ID = 100L;
    private static final long DEV_TG_ID     = 200L;
    private static final long ANON_TG_ID    = 999L;

    private User    manager;
    private User    developer;
    private Project project;

    @BeforeEach
    void setUp() {
        handler = new BotCommandHandler(
                projectService, tasksService, userService,
                projectMemberService, sprintService,
                sprintStoryAssignmentService, taskAssignmentService,
                capabilityService, featureService, userStoryService,
                passwordEncoder, aiService, sessionManager
        );

        manager = new User();
        manager.setId(1L);
        manager.setUsername("alice_mgr");
        manager.setName("Alice Manager");
        manager.setTelegramId(MANAGER_TG_ID);

        developer = new User();
        developer.setId(2L);
        developer.setUsername("bob_dev");
        developer.setName("Bob Developer");
        developer.setTelegramId(DEV_TG_ID);

        project = new Project();
        project.setId(1L);
        project.setName("Andromeda");
        project.setStatus("active");
    }

    // =========================================================================
    // 1. CREATE THE NEXT TASK
    // =========================================================================

    @Nested
    @DisplayName("1. Create the next task")
    class CreateTask {

        @Test
        @DisplayName("Happy path — task is created with correct fields")
        void linkedUser_createsTask_successfully() {
            Tasks saved = buildTask(10L, "Implement login flow", "todo", "high", new BigDecimal("3.0"));

            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.save(any(Tasks.class))).thenReturn(saved);

            String response = handler.handle(
                    "/newtask 1 | Implement login flow | 3.0 | high",
                    MANAGER_TG_ID);

            ArgumentCaptor<Tasks> captor = ArgumentCaptor.forClass(Tasks.class);
            verify(tasksService).save(captor.capture());
            assertEquals("Implement login flow", captor.getValue().getTitle());
            assertEquals("high",  captor.getValue().getPriority());
            assertEquals("todo",  captor.getValue().getStatus());
            assertEquals(0, new BigDecimal("3.0").compareTo(captor.getValue().getEstimatedHours()));
            assertTrue(response.contains("Task created!"));
            assertTrue(response.contains("Implement login flow"));
        }

        @Test
        @DisplayName("4-hour rule — task above limit is rejected with split guidance")
        void estimatedHoursExceedsLimit_rejectsAndSuggestsSplit() {
            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));

            String response = handler.handle(
                    "/newtask 1 | Massive refactor | 8.0 | medium",
                    MANAGER_TG_ID);

            assertTrue(response.contains("exceeds the 4 h limit"));
            assertTrue(response.contains("2 subtasks"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Invalid priority — returns validation error")
        void invalidPriority_returnsError() {
            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));

            String response = handler.handle(
                    "/newtask 1 | Task with bad priority | 2.0 | urgent",
                    MANAGER_TG_ID);

            assertTrue(response.toLowerCase().contains("invalid priority"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Not linked — bot asks user to /link first")
        void notLinked_returnsAuthPrompt() {
            when(userService.findByTelegramId(ANON_TG_ID)).thenReturn(Optional.empty());

            String response = handler.handle("/newtask 1 | Orphan task | 1.0", ANON_TG_ID);

            assertTrue(response.contains("/link"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Project not found — returns not-found error")
        void projectNotFound_returnsError() {
            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));
            when(projectService.findById(99L)).thenReturn(Optional.empty());

            String response = handler.handle(
                    "/newtask 99 | Ghost task | 2.0 | low",
                    MANAGER_TG_ID);

            assertTrue(response.contains("not found"));
            verify(tasksService, never()).save(any());
        }
    }

    // =========================================================================
    // 2. CANCEL A TASK
    // =========================================================================

    @Nested
    @DisplayName("2. Cancel a task")
    class CancelTask {

        @Test
        @DisplayName("Attempt to use 'cancelled' status — bot rejects with valid-values hint")
        void statusCancelled_isNotValid_returnsValidationError() {
            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));

            String response = handler.handle("/taskstatus 10 cancelled", MANAGER_TG_ID);

            assertTrue(response.contains("Invalid status 'cancelled'"));
            assertTrue(response.contains("todo"));
            assertTrue(response.contains("in_progress"));
            assertTrue(response.contains("review"));
            assertTrue(response.contains("done"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Revert in-progress task to todo — soft cancel restores backlog state")
        void revertToTodo_softCancelsInProgressWork() {
            Tasks task = buildTask(10L, "Implement login flow", "in_progress", "high", new BigDecimal("3.0"));

            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));
            when(tasksService.findById(10L)).thenReturn(Optional.of(task));
            when(tasksService.save(any(Tasks.class))).thenReturn(task);

            String response = handler.handle("/taskstatus 10 todo", MANAGER_TG_ID);

            assertTrue(response.contains("in_progress → todo"));
            verify(tasksService).save(any(Tasks.class));
        }

        @Test
        @DisplayName("Task not found — returns not-found error without persisting")
        void taskNotFound_returnsError() {
            when(userService.findByTelegramId(MANAGER_TG_ID)).thenReturn(Optional.of(manager));
            when(tasksService.findById(404L)).thenReturn(Optional.empty());

            String response = handler.handle("/taskstatus 404 todo", MANAGER_TG_ID);

            assertTrue(response.contains("not found"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Not linked — bot asks user to /link first")
        void notLinked_returnsAuthPrompt() {
            when(userService.findByTelegramId(ANON_TG_ID)).thenReturn(Optional.empty());

            String response = handler.handle("/taskstatus 10 todo", ANON_TG_ID);

            assertTrue(response.contains("/link"));
        }
    }

    // =========================================================================
    // 3. ASSIGN A TASK TO A TEAM MEMBER
    // =========================================================================

    @Nested
    @DisplayName("3. Assign a task to a team member")
    class AssignTask {

        @Test
        @DisplayName("Happy path — task linked to sprint, developer auto-assigned")
        void linkedDev_assignsTask_toSprintSuccessfully() {
            Sprint sprint = buildSprint(5L, "Sprint 5");
            Tasks  task   = buildTask(9L, "Fix login", "todo", "high", new BigDecimal("2.0"));
            task.setUserStoryId(77L);

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(sprintService.findById(5L)).thenReturn(Optional.of(sprint));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));
            when(sprintStoryAssignmentService.isStoryActiveInSprint(5L, 77L)).thenReturn(false);
            when(taskAssignmentService.findByTaskIdAndUserId(9L, 2L)).thenReturn(Optional.empty());
            when(tasksService.save(any(Tasks.class))).thenReturn(task);

            String response = handler.handle("/assigntask 5 9", DEV_TG_ID);

            verify(sprintStoryAssignmentService).save(any(SprintStoryAssignment.class));
            verify(taskAssignmentService).save(any(TaskAssignment.class));
            assertTrue(response.contains("Task assigned to sprint!"));
            assertTrue(response.contains("@bob_dev"));
            assertTrue(response.contains("todo → in_progress"));
        }

        @Test
        @DisplayName("Task already in sprint — duplicate assignment is rejected")
        void alreadyInSprint_rejectsDuplicate() {
            Sprint sprint = buildSprint(5L, "Sprint 5");
            Tasks  task   = buildTask(9L, "Fix login", "in_progress", "high", new BigDecimal("2.0"));
            task.setUserStoryId(77L);

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(sprintService.findById(5L)).thenReturn(Optional.of(sprint));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));
            when(sprintStoryAssignmentService.isStoryActiveInSprint(5L, 77L)).thenReturn(true);

            String response = handler.handle("/assigntask 5 9", DEV_TG_ID);

            assertTrue(response.contains("already in sprint"));
            verify(taskAssignmentService, never()).save(any());
        }

        @Test
        @DisplayName("Task has no user story — returns unlinked-story error")
        void taskWithoutUserStory_returnsError() {
            Sprint sprint = buildSprint(5L, "Sprint 5");
            Tasks  task   = buildTask(9L, "Orphan task", "todo", "medium", new BigDecimal("1.0"));
            // userStoryId intentionally null

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(sprintService.findById(5L)).thenReturn(Optional.of(sprint));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));

            String response = handler.handle("/assigntask 5 9", DEV_TG_ID);

            assertTrue(response.contains("not linked to any user story"));
            verify(sprintStoryAssignmentService, never()).save(any());
        }

        @Test
        @DisplayName("Sprint and task belong to different projects — returns cross-project error")
        void differentProjects_returnsCrossProjectError() {
            Project other = new Project();
            other.setId(99L);
            other.setName("Other Project");

            Sprint sprint = buildSprint(5L, "Sprint 5");

            Tasks task = buildTask(9L, "Cross-project task", "todo", "medium", new BigDecimal("1.0"));
            task.setProject(other);

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(sprintService.findById(5L)).thenReturn(Optional.of(sprint));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));

            String response = handler.handle("/assigntask 5 9", DEV_TG_ID);

            assertTrue(response.contains("different projects"));
            verify(sprintStoryAssignmentService, never()).save(any());
        }
    }

    // =========================================================================
    // 4. COMPLETE A TASK
    // =========================================================================

    @Nested
    @DisplayName("4. Complete a task")
    class CompleteTask {

        @Test
        @DisplayName("Happy path — task is marked done with actual hours recorded")
        void linkedDev_completesTask_recordsActualHours() {
            Tasks task = buildTask(9L, "Fix login", "in_progress", "high", new BigDecimal("2.0"));

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));
            when(tasksService.save(any(Tasks.class))).thenReturn(task);

            String response = handler.handle("/completetask 9 2.5", DEV_TG_ID);

            ArgumentCaptor<Tasks> captor = ArgumentCaptor.forClass(Tasks.class);
            verify(tasksService).save(captor.capture());
            assertEquals("done", captor.getValue().getStatus());
            assertEquals(0, new BigDecimal("2.5").compareTo(captor.getValue().getActualHours()));
            assertNotNull(captor.getValue().getActualEnd());
            assertTrue(response.contains("Task completed!"));
            assertTrue(response.contains("in_progress → done"));
        }

        @Test
        @DisplayName("Actual hours under estimate — response shows positive variance")
        void completedUnderEstimate_showsPositiveVariance() {
            Tasks task = buildTask(9L, "Fix login", "in_progress", "high", new BigDecimal("3.0"));

            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(tasksService.findById(9L)).thenReturn(Optional.of(task));
            when(tasksService.save(any(Tasks.class))).thenReturn(task);

            String response = handler.handle("/completetask 9 2.0", DEV_TG_ID);

            assertTrue(response.contains("Task completed!"));
            assertTrue(response.contains("-1.0 h"));
        }

        @Test
        @DisplayName("Task not found — returns not-found error without persisting")
        void taskNotFound_returnsError() {
            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));
            when(tasksService.findById(99L)).thenReturn(Optional.empty());

            String response = handler.handle("/completetask 99 1.0", DEV_TG_ID);

            assertTrue(response.contains("not found"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Zero actual hours — rejected as invalid")
        void zeroActualHours_returnsValidationError() {
            when(userService.findByTelegramId(DEV_TG_ID)).thenReturn(Optional.of(developer));

            String response = handler.handle("/completetask 9 0", DEV_TG_ID);

            assertTrue(response.toLowerCase().contains("usage"));
            verify(tasksService, never()).save(any());
        }

        @Test
        @DisplayName("Not linked — bot asks user to /link first")
        void notLinked_returnsAuthPrompt() {
            when(userService.findByTelegramId(ANON_TG_ID)).thenReturn(Optional.empty());

            String response = handler.handle("/completetask 9 2.0", ANON_TG_ID);

            assertTrue(response.contains("/link"));
            verify(tasksService, never()).save(any());
        }
    }

    // =========================================================================
    // 5. VIEW A DEVELOPER'S TASKS
    // =========================================================================

    @Nested
    @DisplayName("5. View a developer's tasks")
    class ViewDeveloperTasks {

        @Test
        @DisplayName("Project has tasks — returns full task list with statuses")
        void projectWithTasks_returnsTaskList() {
            Tasks t1 = buildTask(1L, "Setup CI pipeline",  "todo",        "high",   new BigDecimal("2.0"));
            Tasks t2 = buildTask(2L, "Write unit tests",   "in_progress", "medium", new BigDecimal("3.5"));
            Tasks t3 = buildTask(3L, "Deploy to staging",  "done",        "low",    new BigDecimal("1.0"));

            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.findByProjectId(1L)).thenReturn(List.of(t1, t2, t3));

            String response = handler.handle("/tasks 1", DEV_TG_ID);

            assertTrue(response.contains("Tasks for project #1 (3)"));
            assertTrue(response.contains("Setup CI pipeline"));
            assertTrue(response.contains("Write unit tests"));
            assertTrue(response.contains("Deploy to staging"));
        }

        @Test
        @DisplayName("Project has no tasks — returns descriptive empty message")
        void noTasks_returnsEmptyMessage() {
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.findByProjectId(1L)).thenReturn(List.of());

            String response = handler.handle("/tasks 1", DEV_TG_ID);

            assertTrue(response.contains("No tasks found for project #1"));
        }

        @Test
        @DisplayName("Project does not exist — returns not-found error")
        void projectNotFound_returnsError() {
            when(projectService.findById(42L)).thenReturn(Optional.empty());

            String response = handler.handle("/tasks 42", DEV_TG_ID);

            assertTrue(response.contains("not found"));
        }

        @Test
        @DisplayName("Single task — task detail command returns all fields")
        void singleTaskDetail_returnsAllFields() {
            Tasks task = buildTask(7L, "Refactor auth service", "review", "critical", new BigDecimal("4.0"));

            when(tasksService.findById(7L)).thenReturn(Optional.of(task));

            String response = handler.handle("/task 7", DEV_TG_ID);

            assertTrue(response.contains("Task #7"));
            assertTrue(response.contains("Refactor auth service"));
            assertTrue(response.contains("review"));
            assertTrue(response.contains("critical"));
        }
    }

    // =========================================================================
    // 6. VIEW A DEVELOPER'S KPIs
    // =========================================================================

    @Nested
    @DisplayName("6. View a developer's KPIs")
    class ViewDeveloperKpis {

        @Test
        @DisplayName("AI enabled — /analyze returns health score and recommendations")
        void aiEnabled_returnsHealthAnalysis() {
            List<Tasks> tasks = List.of(
                    buildTask(1L, "Implement auth",    "done",        "high",     new BigDecimal("2.0")),
                    buildTask(2L, "Fix DB connection", "in_progress", "critical", new BigDecimal("3.0"))
            );

            when(aiService.isEnabled()).thenReturn(true);
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.findByProjectId(1L)).thenReturn(tasks);
            when(sprintService.findByProjectId(1L)).thenReturn(List.of());
            when(projectMemberService.findByProjectId(1L)).thenReturn(List.of());
            when(userStoryService.findByProjectId(1L)).thenReturn(List.of());
            when(aiService.chat(anyString(), anyString()))
                    .thenReturn("Health score: 7/10. Risk: 1 critical task open. Recommend: prioritize DB fix.");

            String response = handler.handle("/analyze 1", DEV_TG_ID);

            assertTrue(response.contains("AI Health Analysis"));
            assertTrue(response.contains("Andromeda"));
            assertTrue(response.contains("Health score"));
        }

        @Test
        @DisplayName("AI disabled — returns disabled message")
        void aiDisabled_returnsDisabledMessage() {
            when(aiService.isEnabled()).thenReturn(false);

            String response = handler.handle("/analyze 1", DEV_TG_ID);

            assertTrue(response.contains("AI is currently disabled"));
        }

        @Test
        @DisplayName("Project not found for KPI query — returns not-found error")
        void projectNotFound_returnsError() {
            when(aiService.isEnabled()).thenReturn(true);
            when(projectService.findById(55L)).thenReturn(Optional.empty());

            String response = handler.handle("/analyze 55", DEV_TG_ID);

            assertTrue(response.contains("not found"));
        }

        @Test
        @DisplayName("AI responds with null — fallback message is shown")
        void aiNullResponse_returnsFallback() {
            when(aiService.isEnabled()).thenReturn(true);
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.findByProjectId(1L)).thenReturn(List.of());
            when(sprintService.findByProjectId(1L)).thenReturn(List.of());
            when(projectMemberService.findByProjectId(1L)).thenReturn(List.of());
            when(userStoryService.findByProjectId(1L)).thenReturn(List.of());
            when(aiService.chat(anyString(), anyString())).thenReturn(null);

            String response = handler.handle("/analyze 1", DEV_TG_ID);

            assertTrue(response.contains("AI did not respond"));
        }
    }

    // =========================================================================
    // 7. MANAGERS CAN VIEW THEIR TEAM'S TASKS
    // =========================================================================

    @Nested
    @DisplayName("7. Managers can view their team's tasks")
    class ManagerViewsTeamTasks {

        @Test
        @DisplayName("Sprint board — manager sees all tasks per sprint with assignees")
        void sprintBoard_showsTasksGroupedBySprintWithAssignees() {
            SprintTaskRow row1 = buildSprintTaskRow(1L, "Implement auth",    "in_progress", "high",   "bob_dev");
            SprintTaskRow row2 = buildSprintTaskRow(2L, "Write unit tests",  "todo",        "medium", "bob_dev");
            SprintTaskRow row3 = buildSprintTaskRow(3L, "Deploy to staging", "done",        "low",    "alice_mgr");

            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintStoryAssignmentService.findSprintBoard(1L)).thenReturn(List.of(row1, row2, row3));

            String response = handler.handle("/sprinttasks 1", MANAGER_TG_ID);

            assertTrue(response.contains("Sprint Board"));
            assertTrue(response.contains("Sprint 1"));
            assertTrue(response.contains("Implement auth"));
            assertTrue(response.contains("Write unit tests"));
            assertTrue(response.contains("Deploy to staging"));
            assertTrue(response.contains("@bob_dev"));
            assertTrue(response.contains("@alice_mgr"));
        }

        @Test
        @DisplayName("No tasks in active sprints — returns descriptive empty message")
        void noActiveSprints_returnsEmptyMessage() {
            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(sprintStoryAssignmentService.findSprintBoard(1L)).thenReturn(List.of());

            String response = handler.handle("/sprinttasks 1", MANAGER_TG_ID);

            assertTrue(response.contains("No tasks found in recent sprints"));
        }

        @Test
        @DisplayName("Full project task list — manager sees all tasks across all sprints")
        void fullProjectTaskList_returnsAllTeamTasks() {
            Tasks t1 = buildTask(1L, "Implement auth",    "in_progress", "high",   new BigDecimal("2.0"));
            Tasks t2 = buildTask(2L, "Write unit tests",  "todo",        "medium", new BigDecimal("1.5"));
            Tasks t3 = buildTask(3L, "Deploy to staging", "done",        "low",    new BigDecimal("1.0"));

            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(tasksService.findByProjectId(1L)).thenReturn(List.of(t1, t2, t3));

            String response = handler.handle("/tasks 1", MANAGER_TG_ID);

            assertTrue(response.contains("Tasks for project #1 (3)"));
            assertTrue(response.contains("Implement auth"));
            assertTrue(response.contains("Write unit tests"));
            assertTrue(response.contains("Deploy to staging"));
        }

        @Test
        @DisplayName("Project members — manager can list all team members and their roles")
        void listMembers_returnsTeamRoster() {
            ProjectMember m1 = new ProjectMember();
            m1.setUser(developer);
            m1.setRole("member");

            ProjectMember m2 = new ProjectMember();
            m2.setUser(manager);
            m2.setRole("manager");

            when(projectService.findById(1L)).thenReturn(Optional.of(project));
            when(projectMemberService.findByProjectId(1L)).thenReturn(List.of(m1, m2));

            String response = handler.handle("/members 1", MANAGER_TG_ID);

            assertTrue(response.contains("Members of project #1 (2)"));
            assertTrue(response.contains("@bob_dev"));
            assertTrue(response.contains("@alice_mgr"));
            assertTrue(response.contains("manager"));
            assertTrue(response.contains("member"));
        }

        @Test
        @DisplayName("Project not found — returns not-found error")
        void projectNotFound_returnsError() {
            when(projectService.findById(77L)).thenReturn(Optional.empty());

            String response = handler.handle("/sprinttasks 77", MANAGER_TG_ID);

            assertTrue(response.contains("not found"));
        }
    }

    // ── helper builders ───────────────────────────────────────────────────────

    private Tasks buildTask(Long id, String title, String status,
                            String priority, BigDecimal estimatedHours) {
        Tasks t = new Tasks();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(priority);
        t.setEstimatedHours(estimatedHours);
        t.setProject(project);
        return t;
    }

    private Sprint buildSprint(Long id, String name) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setName(name);
        s.setProject(project);
        return s;
    }

    private SprintTaskRow buildSprintTaskRow(Long id, String title,
                                             String status, String priority,
                                             String assignee) {
        SprintTaskRow row = mock(SprintTaskRow.class);
        when(row.getId()).thenReturn(id);
        when(row.getTitle()).thenReturn(title);
        when(row.getStatus()).thenReturn(status);
        when(row.getPriority()).thenReturn(priority);
        when(row.getAssignees()).thenReturn(assignee);
        when(row.getSprintName()).thenReturn("Sprint 1");
        when(row.getEstimatedHours()).thenReturn(new BigDecimal("2.0"));
        when(row.getActualHours()).thenReturn(null);
        return row;
    }
}
