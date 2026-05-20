package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserStoryRequest(
        @NotBlank String title,
        @NotNull(message = "is required") Long createdById,
        String description,
        String acceptanceCriteria,
        String priority,
        String status,
        Integer storyPoints,
        Long ownerId
) {}
