package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.dashboard.*;
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
    public CompletableFuture<List<BurndownKPI>> getBurndown(Long projectId) {
        return CompletableFuture.completedFuture(
                sprintStoryRepo.getBurndownByProject(projectId).stream()
                        .map(p -> {
                            long totalStories    = p.getTotalStories()    != null ? p.getTotalStories()    : 0L;
                            long completedStories = p.getCompletedStories() != null ? p.getCompletedStories() : 0L;
                            long totalTasks      = p.getTotalTasks()      != null ? p.getTotalTasks()      : 0L;
                            long completedTasks  = p.getCompletedTasks()  != null ? p.getCompletedTasks()  : 0L;
                            long totalPoints     = p.getTotalPoints()     != null ? p.getTotalPoints()     : 0L;
                            long completedPoints = p.getCompletedPoints() != null ? p.getCompletedPoints() : 0L;
                            return BurndownKPI.builder()
                                    .sprintName(p.getSprintName())
                                    .totalStories(totalStories)
                                    .completedStories(completedStories)
                                    .remainingStories(totalStories - completedStories)
                                    .totalTasks(totalTasks)
                                    .completedTasks(completedTasks)
                                    .remainingTasks(totalTasks - completedTasks)
                                    .totalPoints(totalPoints)
                                    .completedPoints(completedPoints)
                                    .remainingPoints(totalPoints - completedPoints)
                                    .build();
                        })
                        .toList()
        );
    }

    @Async
    public CompletableFuture<List<HoursPerUserKPI>> getHoursPerUser(Long projectId) {
        return CompletableFuture.completedFuture(
                sprintStoryRepo.getHoursPerUserByProject(projectId).stream()
                        .map(p -> HoursPerUserKPI.builder()
                                .sprintName(p.getSprintName())
                                .userName(p.getUserName())
                                .actualHours(p.getActualHours())
                                .estimatedHours(p.getEstimatedHours())
                                .build())
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