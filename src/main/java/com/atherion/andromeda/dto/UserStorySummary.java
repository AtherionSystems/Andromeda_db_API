package com.atherion.andromeda.dto;

public record UserStorySummary(
        Long id,
        String title,
        String priority,
        String status,
        Integer storyPoints,
        String featureName,
        String ownerName
) {}
