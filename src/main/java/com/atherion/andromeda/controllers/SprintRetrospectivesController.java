package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintRetrospective;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.SprintRetrospectiveService;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints/{sprintId}/retrospective")
@RequiredArgsConstructor
public class SprintRetrospectivesController {

    private final SprintRetrospectiveService sprintRetrospectiveService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getBySprint(@PathVariable Long projectId, @PathVariable Long sprintId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint not found"));
        }
        return sprintRetrospectiveService.findBySprintId(sprintId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint retrospective not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long sprintId,
                                    @RequestBody Map<String, Object> payload) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint not found"));
        }
        if (sprintRetrospectiveService.findBySprintId(sprintId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Sprint already has a retrospective"));
        }
        Long createdById = asLong(payload.get("createdById"));
        if (createdById == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "createdById is required"));
        }
        User createdBy = userService.findById(createdById).orElse(null);
        if (createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "createdBy user not found"));
        }

        SprintRetrospective retrospective = new SprintRetrospective();
        retrospective.setSprint(sprint);
        retrospective.setSummary(asString(payload.get("summary")));
        retrospective.setWhatWentWell(asString(payload.get("whatWentWell")));
        retrospective.setWhatWentWrong(asString(payload.get("whatWentWrong")));
        retrospective.setCreatedBy(createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintRetrospectiveService.save(retrospective));
    }

    @PatchMapping
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long sprintId,
                                   @RequestBody Map<String, Object> payload) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint not found"));
        }
        SprintRetrospective retrospective = sprintRetrospectiveService.findBySprintId(sprintId).orElse(null);
        if (retrospective == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint retrospective not found"));
        }

        if (payload.containsKey("summary")) retrospective.setSummary(asString(payload.get("summary")));
        if (payload.containsKey("whatWentWell")) retrospective.setWhatWentWell(asString(payload.get("whatWentWell")));
        if (payload.containsKey("whatWentWrong")) retrospective.setWhatWentWrong(asString(payload.get("whatWentWrong")));
        if (payload.containsKey("updatedById")) {
            Long updatedById = asLong(payload.get("updatedById"));
            if (updatedById != null) {
                User updatedBy = userService.findById(updatedById).orElse(null);
                if (updatedBy == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
                }
                retrospective.setUpdatedBy(updatedBy);
            }
            retrospective.setUpdatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(sprintRetrospectiveService.save(retrospective));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long sprintId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint not found"));
        }
        SprintRetrospective retrospective = sprintRetrospectiveService.findBySprintId(sprintId).orElse(null);
        if (retrospective == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sprint retrospective not found"));
        }
        sprintRetrospectiveService.deleteById(retrospective.getId());
        return ResponseEntity.noContent().build();
    }

    private Sprint findSprintInProject(Long projectId, Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
