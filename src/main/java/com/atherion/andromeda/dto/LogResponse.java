package com.atherion.andromeda.dto;

import com.atherion.andromeda.model.Log;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        Long userId,
        String entity,
        Long entityId,
        String action,
        String detail,
        LocalDateTime logDate
) {
    public static LogResponse from(Log log) {
        return new LogResponse(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getEntity(),
                log.getEntityId(),
                log.getAction(),
                log.getDetail(),
                log.getLogDate()
        );
    }
}
