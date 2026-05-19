package com.atherion.andromeda.dto;

public record UpdateStorySpilloverRequest(
        String reason,
        String detail,
        Long updatedById
) {}
