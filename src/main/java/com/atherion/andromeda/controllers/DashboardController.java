package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.dashboard.*;
import com.atherion.andromeda.services.KpiService;
import lombok.RequiredArgsConstructor;
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

        CompletableFuture<List<CompletionRateKPI>>     completionFuture = kpiService.getCompletionRate(projectId);
        CompletableFuture<List<TeamVelocityKPI>>       velocityFuture   = kpiService.getTeamVelocity(projectId);
        CompletableFuture<List<TaskDistributionKPI>>   distFuture       = kpiService.getTaskDistribution(projectId);
        CompletableFuture<List<UserTasksPerSprintKPI>> userFuture       = kpiService.getUserTasksPerSprint(projectId);

        CompletableFuture.allOf(
                completionFuture, velocityFuture, distFuture, userFuture
        ).join();

        return ResponseEntity.ok(
                DashboardResponse.builder()
                        .projectId(projectId)
                        .generatedAt(LocalDateTime.now())
                        .completionRateBySprint(completionFuture.get())
                        .teamVelocity(velocityFuture.get())
                        .taskDistribution(distFuture.get())
                        .userTasksPerSprint(userFuture.get())
                        .build()
        );
    }
}
