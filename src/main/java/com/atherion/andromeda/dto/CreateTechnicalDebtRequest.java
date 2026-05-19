package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTechnicalDebtRequest(
        @NotBlank String title,
        @NotBlank String debtType,
        @NotNull Long assignedToId,
        @NotNull Long createdById,
        String description,
        String priority,
        String status,
        Long userStoryId,
        Long taskId
) {}
