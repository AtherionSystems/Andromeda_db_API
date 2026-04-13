package com.atherion.andromeda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateProjectMemberRequest(
        @NotNull Long projectId,
        @NotNull Long userId,
        @Pattern(regexp = "owner|manager|member", message = "Role must be owner, manager, or member") String role
) {}

