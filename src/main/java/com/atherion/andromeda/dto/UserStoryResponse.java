package com.atherion.andromeda.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserStoryResponse(
        Long id,
        String title,
        String description,
        String acceptanceCriteria,
        String priority,
        String status,
        Integer storyPoints,
        Long featureId,
        String featureName,
        String ownerName,
        String createdByName,
        String updatedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AssignedUserSummary> assignedUsers
) {
    public UserStoryResponse(Long id, String title, String description, String acceptanceCriteria,
                             String priority, String status, Integer storyPoints,
                             Long featureId, String featureName,
                             String ownerName, String createdByName, String updatedByName,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, description, acceptanceCriteria, priority, status, storyPoints,
                featureId, featureName, ownerName, createdByName, updatedByName,
                createdAt, updatedAt, List.of());
    }

    public UserStoryResponse withAssignedUsers(List<AssignedUserSummary> assignedUsers) {
        return new UserStoryResponse(id, title, description, acceptanceCriteria, priority, status,
                storyPoints, featureId, featureName, ownerName, createdByName, updatedByName,
                createdAt, updatedAt, assignedUsers);
    }
}
