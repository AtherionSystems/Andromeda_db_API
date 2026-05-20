package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSprintStoryAssignmentRequest(
        @NotNull(message = "is required") Long userStoryId
) {}
