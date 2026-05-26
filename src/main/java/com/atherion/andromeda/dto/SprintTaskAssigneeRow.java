package com.atherion.andromeda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SprintTaskAssigneeRow(
        Long userStoryId,
        Long taskId,
        String taskTitle,
        String taskPriority,
        String taskStatus,
        LocalDateTime taskDueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        Long assigneeUserId,
        String assigneeUserName
) {}
