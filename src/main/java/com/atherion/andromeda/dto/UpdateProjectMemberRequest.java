package com.atherion.andromeda.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateProjectMemberRequest(
        Long projectId,
        Long userId,
        @Pattern(regexp = "owner|manager|member", message = "Role must be owner, manager, or member") String role
) {}

