package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStorySpilloverRequest(
        @NotNull Long sprintStoryId,
        @NotNull Long userStoryId,
        @NotNull Long originSprintId,
        @NotNull Long destinationSprintId,
        @NotNull Long createdById,
        @NotBlank String reason,
        String detail
) {}
