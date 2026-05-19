package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record AssignUserToTaskRequest(
        @NotNull Long userId
) {}
