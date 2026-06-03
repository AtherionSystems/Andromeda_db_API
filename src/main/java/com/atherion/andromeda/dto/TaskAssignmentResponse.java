package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.TaskAssignment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskAssignmentResponse(
        Long id,
        Long taskId,
        String taskTitle,
        String taskStatus,
        String taskPriority,
        BigDecimal taskEstimatedHours,
        BigDecimal taskActualHours,
        Long taskUserStoryId,
        Long userId,
        String userName,
        String userUsername,
        String userEmail,
        LocalDateTime assignedAt
) {
    public static TaskAssignmentResponse from(TaskAssignment ta) {
        return new TaskAssignmentResponse(
                ta.getId(),
                ta.getTask().getId(),
                ta.getTask().getTitle(),
                ta.getTask().getStatus(),
                ta.getTask().getPriority(),
                ta.getTask().getEstimatedHours(),
                ta.getTask().getActualHours(),
                ta.getTask().getUserStoryId(),
                ta.getUser().getId(),
                ta.getUser().getName(),
                ta.getUser().getUsername(),
                ta.getUser().getEmail(),
                ta.getAssignedAt()
        );
    }
}
