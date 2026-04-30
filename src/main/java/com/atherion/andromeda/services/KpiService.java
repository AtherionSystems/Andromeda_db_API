package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.dashboard.*;
import com.atherion.andromeda.projections.CompletionRateProjection;
import com.atherion.andromeda.projections.TaskDistributionProjection;
import com.atherion.andromeda.repositories.SprintStoryAssignmentRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final SprintStoryAssignmentRepository sprintStoryRepo;
    private final TasksRepository                 tasksRepository;

    @Async
    public CompletableFuture<List<CompletionRateKPI>> getCompletionRate(Long projectId) {
        return CompletableFuture.completedFuture(
                sprintStoryRepo.getCompletionRateByProject(projectId).stream()
                        .map(p -> {
                            long total     = p.getTotalStories()     != null ? p.getTotalStories()     : 0L;
                            long completed = p.getCompletedStories() != null ? p.getCompletedStories() : 0L;
                            double rate    = total > 0
                                    ? Math.round(completed * 100.0 / total * 100.0) / 100.0
                                    : 0.0;
                            return CompletionRateKPI.builder()
                                    .sprintName(p.getSprintName())
                                    .totalStories(total)
                                    .completedStories(completed)
                                    .completionRate(rate)
                                    .build();
                        })
                        .toList()
        );
    }

    @Async
    public CompletableFuture<List<TeamVelocityKPI>> getTeamVelocity(Long projectId) {
        return CompletableFuture.completedFuture(
                sprintStoryRepo.getTeamVelocityByProject(projectId).stream()
                        .map(p -> TeamVelocityKPI.builder()
                                .sprintName(p.getSprintName())
                                .pointsCompleted(p.getPointsCompleted() != null ? p.getPointsCompleted() : 0L)
                                .pointsPlanned(p.getPointsPlanned()     != null ? p.getPointsPlanned()   : 0L)
                                .build())
                        .toList()
        );
    }

    @Async
    public CompletableFuture<List<TaskDistributionKPI>> getTaskDistribution(Long projectId) {
        return CompletableFuture.completedFuture(
                tasksRepository.getTaskDistributionByProject(projectId).stream()
                        .map(p -> TaskDistributionKPI.builder()
                                .status(p.getStatus())
                                .total(p.getTotal() != null ? p.getTotal() : 0L)
                                .build())
                        .toList()
        );
    }

    @Async
    public CompletableFuture<List<UserTasksPerSprintKPI>> getUserTasksPerSprint(Long projectId) {
        return CompletableFuture.completedFuture(
                sprintStoryRepo.getUserTasksPerSprint(projectId).stream()
                        .map(p -> UserTasksPerSprintKPI.builder()
                                .sprintName(p.getSprintName())
                                .userName(p.getUserName())
                                .tasksCompleted(p.getTasksCompleted() != null ? p.getTasksCompleted() : 0L)
                                .build())
                        .toList()
        );
    }
}