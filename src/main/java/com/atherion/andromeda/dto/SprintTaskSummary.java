package com.atherion.andromeda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SprintTaskSummary(
        Long id,
        String title,
        String priority,
        String status,
        LocalDateTime dueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        List<AssignedUserSummary> assignees
) {}
