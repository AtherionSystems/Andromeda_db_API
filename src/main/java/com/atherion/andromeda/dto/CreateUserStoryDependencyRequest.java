package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record CreateUserStoryDependencyRequest(
        @NotNull(message = "is required") Long blockedById,
        String dependencyType
) {}
