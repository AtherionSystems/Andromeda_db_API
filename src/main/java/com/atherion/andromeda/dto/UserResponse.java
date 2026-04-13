package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String username,
        String email,
        String phone,
        Long userTypeId,
        String userType,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType().getId(),
                user.getUserType().getUserType(),
                user.getCreatedAt()
        );
    }
}
