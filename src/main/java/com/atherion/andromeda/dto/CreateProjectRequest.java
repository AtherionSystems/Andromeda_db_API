package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Pattern(regexp = "active|paused|completed|cancelled") String status,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}