package com.atherion.andromeda.dto;

public record UpdateSprintRetrospectiveRequest(
        String summary,
        String whatWentWell,
        String whatWentWrong,
        Long updatedById
) {}
