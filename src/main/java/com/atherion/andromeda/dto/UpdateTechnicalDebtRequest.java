package com.atherion.andromeda.dto;

public record UpdateTechnicalDebtRequest(
        String title,
        String description,
        String debtType,
        String priority,
        String status,
        String resolvedAt,
        Long assignedToId,
        Long updatedById
) {}
