package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.ProjectMember;

import java.time.Instant;
import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Long id,
        Long projectId,
        String projectName,
        Long userId,
        String username,
        String role,
        LocalDateTime joinedAt
) {
    public static ProjectMemberResponse from(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getProject().getId(),
                member.getProject().getName(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}

