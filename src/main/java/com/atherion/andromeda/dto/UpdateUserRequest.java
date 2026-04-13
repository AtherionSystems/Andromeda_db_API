package com.atherion.andromeda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 255) String name,
        @Size(max = 50)  String username,
                         String password,
        @Email @Size(max = 255) String email,
        @Size(max = 20)  String phone,
                         Long userTypeId
) {}
