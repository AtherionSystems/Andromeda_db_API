package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSprintRetrospectiveRequest(
        @NotNull(message = "is required") Long createdById,
        String summary,
        String whatWentWell,
        String whatWentWrong
) {}
