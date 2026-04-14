package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateLogRequest(
        Long userId,
        @NotBlank @Size(max = 50) String entity,
        Long entityId,
        @NotBlank @Size(max = 50) String action,
        @Size(max = 1000) String detail,
        LocalDateTime logDate
) {}