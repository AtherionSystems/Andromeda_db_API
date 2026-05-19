package com.atherion.andromeda.dto;

public record UpdateSprintTaskRequest(
        String removedAt,
        Long movedToId
) {}
