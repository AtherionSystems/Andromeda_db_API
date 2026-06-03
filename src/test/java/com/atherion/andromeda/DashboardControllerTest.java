package com.atherion.andromeda;

import com.atherion.andromeda.controllers.DashboardController;
import com.atherion.andromeda.dto.dashboard.*;
import com.atherion.andromeda.services.KpiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private KpiService kpiService;
    @InjectMocks private DashboardController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Stubs all KpiService calls for a given projectId with empty lists. */
    private void stubAllEmpty(Long projectId) {
        when(kpiService.getBurndown(projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTeamVelocity(projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTaskDistribution(projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getUserTasksPerSprint(projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getHoursPerUser(projectId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
    }

    // ── GET /api/dashboard ─────────────────────────────────────────────────────

    @Test
    void getDashboard_validProjectId_returns200WithAllFields() throws Exception {
        stubAllEmpty(1L);

        mockMvc.perform(get("/api/dashboard").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.burndownBySprint").isArray())
                .andExpect(jsonPath("$.teamVelocity").isArray())
                .andExpect(jsonPath("$.taskDistribution").isArray())
                .andExpect(jsonPath("$.userTasksPerSprint").isArray())
                .andExpect(jsonPath("$.hoursPerUserBySprint").isArray());
    }

    @Test
    void getDashboard_missingProjectId_returns400() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isBadRequest());
    }

    // ── burndown data ──────────────────────────────────────────────────────────

    @Test
    void getDashboard_withBurndownData_returnsCorrectValues() throws Exception {
        BurndownKPI burndown = BurndownKPI.builder()
                .sprintName("Sprint 1")
                .totalStories(10L).completedStories(6L).remainingStories(4L)
                .totalTasks(20L).completedTasks(12L).remainingTasks(8L)
                .totalPoints(50L).completedPoints(30L).remainingPoints(20L)
                .build();

        when(kpiService.getBurndown(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of(burndown)));
        when(kpiService.getTeamVelocity(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTaskDistribution(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getUserTasksPerSprint(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getHoursPerUser(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/dashboard").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.burndownBySprint[0].sprintName").value("Sprint 1"))
                .andExpect(jsonPath("$.burndownBySprint[0].totalStories").value(10))
                .andExpect(jsonPath("$.burndownBySprint[0].completedStories").value(6))
                .andExpect(jsonPath("$.burndownBySprint[0].remainingStories").value(4))
                .andExpect(jsonPath("$.burndownBySprint[0].remainingPoints").value(20));
    }

    // ── velocity data ──────────────────────────────────────────────────────────

    @Test
    void getDashboard_withVelocityData_returnsCorrectValues() throws Exception {
        TeamVelocityKPI velocity = TeamVelocityKPI.builder()
                .sprintName("Sprint 2")
                .pointsCompleted(35L)
                .pointsPlanned(40L)
                .build();

        when(kpiService.getBurndown(2L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTeamVelocity(2L))
                .thenReturn(CompletableFuture.completedFuture(List.of(velocity)));
        when(kpiService.getTaskDistribution(2L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getUserTasksPerSprint(2L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getHoursPerUser(2L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/dashboard").param("projectId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamVelocity[0].sprintName").value("Sprint 2"))
                .andExpect(jsonPath("$.teamVelocity[0].pointsCompleted").value(35))
                .andExpect(jsonPath("$.teamVelocity[0].pointsPlanned").value(40));
    }

    // ── task distribution data ─────────────────────────────────────────────────

    @Test
    void getDashboard_withTaskDistributionData_returnsCorrectValues() throws Exception {
        TaskDistributionKPI dist = TaskDistributionKPI.builder()
                .status("in_progress")
                .total(7L)
                .build();

        when(kpiService.getBurndown(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTeamVelocity(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getTaskDistribution(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of(dist)));
        when(kpiService.getUserTasksPerSprint(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(kpiService.getHoursPerUser(1L))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        mockMvc.perform(get("/api/dashboard").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskDistribution[0].status").value("in_progress"))
                .andExpect(jsonPath("$.taskDistribution[0].total").value(7));
    }
}
