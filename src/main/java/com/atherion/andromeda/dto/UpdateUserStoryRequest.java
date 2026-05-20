package com.atherion.andromeda.dto;

public record UpdateUserStoryRequest(
        String title,
        String description,
        String acceptanceCriteria,
        String priority,
        String status,
        Integer storyPoints,
        Long ownerId,
        Long updatedById
) {}
