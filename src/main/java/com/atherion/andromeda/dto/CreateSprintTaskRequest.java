package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSprintTaskRequest(
        @NotNull Long taskId
) {}
