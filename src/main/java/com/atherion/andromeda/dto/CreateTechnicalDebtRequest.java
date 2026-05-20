package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTechnicalDebtRequest(
        @NotBlank String title,
        @NotBlank String debtType,
        @NotNull(message = "is required") Long assignedToId,
        @NotNull(message = "is required") Long createdById,
        String description,
        String priority,
        String status,
        Long userStoryId,
        Long taskId
) {}
