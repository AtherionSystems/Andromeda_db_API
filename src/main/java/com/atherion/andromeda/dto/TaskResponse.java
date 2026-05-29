package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.Tasks;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String priority,
        String status,
        LocalDateTime startDate,
        LocalDateTime dueDate,
        LocalDateTime actualEnd,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        Long userStoryId,
        String projectName,
        String assignedUserName
) {
    public static TaskResponse from(Tasks t) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getStartDate(),
                t.getDueDate(),
                t.getActualEnd(),
                t.getEstimatedHours(),
                t.getActualHours(),
                t.getUserStoryId(),
                t.getProject().getName(),
                null
        );
    }
}
