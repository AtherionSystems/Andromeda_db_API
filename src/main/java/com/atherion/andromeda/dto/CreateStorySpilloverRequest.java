package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStorySpilloverRequest(
        @NotNull(message = "is required") Long sprintStoryId,
        @NotNull(message = "is required") Long userStoryId,
        @NotNull(message = "is required") Long originSprintId,
        @NotNull(message = "is required") Long destinationSprintId,
        @NotNull(message = "is required") Long createdById,
        @NotBlank String reason,
        String detail
) {}
