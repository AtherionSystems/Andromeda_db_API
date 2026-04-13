package com.atherion.andromeda.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 50)  String username,
        @NotBlank                  String password,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20)            String phone,
        @NotNull                   Long userTypeId
) {}
