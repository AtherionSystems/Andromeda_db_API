package com.atherion.andromeda.dto;

public record UpdateSprintStoryAssignmentRequest(
        String removedAt,
        Long movedToId
) {}
