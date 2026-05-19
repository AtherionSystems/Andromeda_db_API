package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.dashboard.*;
import com.atherion.andromeda.services.KpiService;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final KpiService kpiService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam Long projectId) throws Exception {

        CompletableFuture<List<BurndownKPI>>           burndownFuture   = kpiService.getBurndown(projectId);
        CompletableFuture<List<TeamVelocityKPI>>       velocityFuture   = kpiService.getTeamVelocity(projectId);
        CompletableFuture<List<TaskDistributionKPI>>   distFuture       = kpiService.getTaskDistribution(projectId);
        CompletableFuture<List<UserTasksPerSprintKPI>> userFuture       = kpiService.getUserTasksPerSprint(projectId);
        CompletableFuture<List<HoursPerUserKPI>>       hoursFuture      = kpiService.getHoursPerUser(projectId);

        CompletableFuture.allOf(
                burndownFuture, velocityFuture, distFuture, userFuture, hoursFuture
        ).join();

        return ResponseEntity.ok(
                DashboardResponse.builder()
                        .projectId(projectId)
                        .generatedAt(LocalDateTime.now())
                        .burndownBySprint(burndownFuture.get())
                        .teamVelocity(velocityFuture.get())
                        .taskDistribution(distFuture.get())
                        .userTasksPerSprint(userFuture.get())
                        .hoursPerUserBySprint(hoursFuture.get())
                        .build()
        );
    }
}
