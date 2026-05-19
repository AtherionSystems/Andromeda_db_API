package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record CreateUserStoryDependencyRequest(
        @NotNull Long blockedById,
        String dependencyType
) {}
