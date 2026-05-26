package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.Sprint;

import java.time.LocalDateTime;

public record SprintResponse(
        Long id,
        String name,
        String goal,
        String status,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        LocalDateTime actualEnd,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long projectId,
        String projectName
) {
    public static SprintResponse from(Sprint s) {
        return new SprintResponse(
                s.getId(), s.getName(), s.getGoal(), s.getStatus(),
                s.getStartDate(), s.getDueDate(), s.getActualEnd(),
                s.getCreatedAt(), s.getUpdatedAt(),
                s.getProject().getId(), s.getProject().getName()
        );
    }
}
