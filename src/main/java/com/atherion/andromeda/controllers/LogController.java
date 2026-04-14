package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateLogRequest;
import com.atherion.andromeda.dto.LogResponse;
import com.atherion.andromeda.model.Log;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.LogService;
import com.atherion.andromeda.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;
    private final UserService userService;

    @GetMapping({"/api/logs", "/logs"})
    public ResponseEntity<List<LogResponse>> getLogs(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<LogResponse> logs = logService.search(projectId, taskId, userId, from, to).stream()
                .map(LogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    @GetMapping({"/api/projects/{projectId}/logs", "/projects/{projectId}/logs"})
    public ResponseEntity<List<LogResponse>> getProjectLogs(@PathVariable Long projectId) {
        List<LogResponse> logs = logService.findByProjectId(projectId).stream()
                .map(LogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }

    @PostMapping({"/api/logs", "/logs"})
    public ResponseEntity<?> create(@Valid @RequestBody CreateLogRequest request) {
        User user = null;
        if (request.userId() != null) {
            user = userService.findById(request.userId()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }
        }

        Log log = new Log();
        log.setUser(user);
        log.setEntity(request.entity());
        log.setEntityId(request.entityId());
        log.setAction(request.action());
        log.setDetail(request.detail());
        log.setLogDate(request.logDate() == null ? LocalDateTime.now() : request.logDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(LogResponse.from(logService.save(log)));
    }
}