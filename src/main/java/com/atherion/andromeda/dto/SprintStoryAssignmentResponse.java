package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.SprintStoryAssignment;

import java.time.LocalDateTime;
import java.util.List;

public record SprintStoryAssignmentResponse(
        Long id,
        Long sprintId,
        String sprintName,
        Long userStoryId,
        LocalDateTime addedAt,
        LocalDateTime removedAt,
        Integer isActive,
        Long movedToSprintId,
        String movedToSprintName,
        UserStorySummary userStory,
        List<SprintTaskSummary> tasks
) {
    // Constructor usado por el JPQL constructor expression (sin userStory ni tasks)
    public SprintStoryAssignmentResponse(Long id, Long sprintId, String sprintName,
                                         Long userStoryId, LocalDateTime addedAt, LocalDateTime removedAt,
                                         Integer isActive, Long movedToSprintId, String movedToSprintName) {
        this(id, sprintId, sprintName, userStoryId, addedAt, removedAt,
                isActive, movedToSprintId, movedToSprintName, null, List.of());
    }

    public SprintStoryAssignmentResponse withDetails(UserStorySummary userStory, List<SprintTaskSummary> tasks) {
        return new SprintStoryAssignmentResponse(id, sprintId, sprintName, userStoryId,
                addedAt, removedAt, isActive, movedToSprintId, movedToSprintName, userStory, tasks);
    }

    public static SprintStoryAssignmentResponse from(SprintStoryAssignment ssa) {
        return new SprintStoryAssignmentResponse(
                ssa.getId(),
                ssa.getSprint().getId(),
                ssa.getSprint().getName(),
                ssa.getUserStoryId(),
                ssa.getAddedAt(),
                ssa.getRemovedAt(),
                ssa.getIsActive(),
                ssa.getMovedTo() != null ? ssa.getMovedTo().getId() : null,
                ssa.getMovedTo() != null ? ssa.getMovedTo().getName() : null
        );
    }
}
