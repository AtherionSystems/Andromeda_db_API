package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record AssignUserToTaskRequest(
        @NotNull(message = "is required") Long userId
) {}
