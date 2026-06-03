package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.User;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        Long id,
        String name,
        String username,
        String email,
        String phone,
        Long userTypeId,
        String userType,
        LocalDateTime createdAt
) {
    public static LoginResponse from(User user, String token) {
        return new LoginResponse(
                token,
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
