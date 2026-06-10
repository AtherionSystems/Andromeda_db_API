package com.atherion.andromeda;

import com.atherion.andromeda.controllers.MeController;
import com.atherion.andromeda.dto.TaskResponse;
import com.atherion.andromeda.dto.dashboard.MyHoursPerSprintKPI;
import com.atherion.andromeda.dto.dashboard.MyTasksPerSprintKPI;
import com.atherion.andromeda.dto.dashboard.TaskDistributionKPI;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.ProjectMemberRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.services.KpiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock private UserRepository           userRepository;
    @Mock private ProjectMemberRepository  projectMemberRepository;
    @Mock private TasksRepository          tasksRepository;
    @Mock private KpiService               kpiService;

    @InjectMocks private MeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        // Simulates the non-prod JwtAuthFilter: principal is a plain String (username)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null, Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("Test User");
        u.setUsername("testuser");
        u.setEmail("testuser@example.com");
        u.setPasswordHash("hashed");
        return u;
    }

    private Project buildProject(Long id, String name) {
        Project p = new Project();
        p.setId(id);
        p.setName(name);
        p.setStatus("active");
        return p;
    }

    private TaskResponse buildTaskResponse(Long id, String title, String projectName) {
        return new TaskResponse(id, title, null, "medium", "todo",
                null, null, null, null, null, null, projectName, "testuser");
    }

    private void stubUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(buildUser(1L)));
    }

    private void stubUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
    }

    private void stubEmptyDashboard(Long userId, Long projectId) {
        when(kpiService.getMyTaskDistribution(userId, projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyHoursPerSprint(userId, projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyTasksPerSprint(userId, projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
    }

    // ── GET /api/me/projects ───────────────────────────────────────────────────

    @Test
    void getMyProjects_authenticated_returns200WithProjects() throws Exception {
        stubUser();
        when(projectMemberRepository.findProjectsByUserId(1L))
                .thenReturn(List.of(buildProject(10L, "Alpha"), buildProject(11L, "Beta")));

        mockMvc.perform(get("/api/me/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name").value("Beta"));
    }

    @Test
    void getMyProjects_noProjects_returns200WithEmptyList() throws Exception {
        stubUser();
        when(projectMemberRepository.findProjectsByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/me/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyProjects_unknownUser_returns401() throws Exception {
        stubUserNotFound();

        mockMvc.perform(get("/api/me/projects"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/me/tasks ──────────────────────────────────────────────────────

    @Test
    void getMyTasks_noFilters_returns200WithAllTasks() throws Exception {
        stubUser();
        when(tasksRepository.findByAssignedUserIdAsResponse(1L, null, null))
                .thenReturn(List.of(
                        buildTaskResponse(1L, "Task A", "Alpha"),
                        buildTaskResponse(2L, "Task B", "Beta")));

        mockMvc.perform(get("/api/me/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Task A"))
                .andExpect(jsonPath("$[0].projectName").value("Alpha"))
                .andExpect(jsonPath("$[1].title").value("Task B"));
    }

    @Test
    void getMyTasks_withProjectIdFilter_passesFilterToRepository() throws Exception {
        stubUser();
        when(tasksRepository.findByAssignedUserIdAsResponse(1L, 10L, null))
                .thenReturn(List.of(buildTaskResponse(1L, "Alpha Task", "Alpha")));

        mockMvc.perform(get("/api/me/tasks").param("projectId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].projectName").value("Alpha"));
    }

    @Test
    void getMyTasks_withStatusFilter_passesFilterToRepository() throws Exception {
        stubUser();
        when(tasksRepository.findByAssignedUserIdAsResponse(1L, null, "done"))
                .thenReturn(List.of(buildTaskResponse(5L, "Done Task", "Alpha")));

        mockMvc.perform(get("/api/me/tasks").param("status", "done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].title").value("Done Task"));
    }

    @Test
    void getMyTasks_withBothFilters_returns200() throws Exception {
        stubUser();
        when(tasksRepository.findByAssignedUserIdAsResponse(1L, 10L, "in_progress"))
                .thenReturn(List.of(buildTaskResponse(3L, "In Progress Task", "Alpha")));

        mockMvc.perform(get("/api/me/tasks").param("projectId", "10").param("status", "in_progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].title").value("In Progress Task"));
    }

    @Test
    void getMyTasks_emptyResult_returns200WithEmptyList() throws Exception {
        stubUser();
        when(tasksRepository.findByAssignedUserIdAsResponse(1L, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/me/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyTasks_unknownUser_returns401() throws Exception {
        stubUserNotFound();

        mockMvc.perform(get("/api/me/tasks"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/me/dashboard ──────────────────────────────────────────────────

    @Test
    void getDashboard_missingProjectId_returns400() throws Exception {
        mockMvc.perform(get("/api/me/dashboard"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboard_validRequest_returns200WithStructure() throws Exception {
        stubUser();
        stubEmptyDashboard(1L, 5L);

        mockMvc.perform(get("/api/me/dashboard").param("projectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName").value("Test User"))
                .andExpect(jsonPath("$.projectId").value(5))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.myTaskDistribution").isArray())
                .andExpect(jsonPath("$.myHoursPerSprint").isArray())
                .andExpect(jsonPath("$.myTasksPerSprint").isArray());
    }

    @Test
    void getDashboard_withTaskDistributionData_returnsCorrectValues() throws Exception {
        stubUser();
        TaskDistributionKPI dist = TaskDistributionKPI.builder().status("done").total(8L).build();
        when(kpiService.getMyTaskDistribution(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of(dist)));
        when(kpiService.getMyHoursPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyTasksPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/me/dashboard").param("projectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myTaskDistribution.length()").value(1))
                .andExpect(jsonPath("$.myTaskDistribution[0].status").value("done"))
                .andExpect(jsonPath("$.myTaskDistribution[0].total").value(8));
    }

    @Test
    void getDashboard_withHoursData_returnsCorrectValues() throws Exception {
        stubUser();
        MyHoursPerSprintKPI hours = MyHoursPerSprintKPI.builder()
                .sprintName("Sprint 1")
                .actualHours(new BigDecimal("12.5"))
                .estimatedHours(new BigDecimal("16.0"))
                .build();
        when(kpiService.getMyTaskDistribution(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyHoursPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of(hours)));
        when(kpiService.getMyTasksPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/me/dashboard").param("projectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myHoursPerSprint[0].sprintName").value("Sprint 1"))
                .andExpect(jsonPath("$.myHoursPerSprint[0].actualHours").value(12.5))
                .andExpect(jsonPath("$.myHoursPerSprint[0].estimatedHours").value(16.0));
    }

    @Test
    void getDashboard_withTasksPerSprintData_returnsCorrectValues() throws Exception {
        stubUser();
        MyTasksPerSprintKPI tasks = MyTasksPerSprintKPI.builder()
                .sprintName("Sprint 2")
                .tasksCompleted(7L)
                .build();
        when(kpiService.getMyTaskDistribution(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyHoursPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getMyTasksPerSprint(1L, 5L))
                .thenReturn(CompletableFuture.completedFuture(List.of(tasks)));

        mockMvc.perform(get("/api/me/dashboard").param("projectId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myTasksPerSprint[0].sprintName").value("Sprint 2"))
                .andExpect(jsonPath("$.myTasksPerSprint[0].tasksCompleted").value(7));
    }

    @Test
    void getDashboard_unknownUser_returns401() throws Exception {
        stubUserNotFound();

        mockMvc.perform(get("/api/me/dashboard").param("projectId", "5"))
                .andExpect(status().isUnauthorized());
    }
}
