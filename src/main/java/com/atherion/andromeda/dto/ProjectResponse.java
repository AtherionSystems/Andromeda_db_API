package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.Project;

import java.time.Instant;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt()
        );
    }
}