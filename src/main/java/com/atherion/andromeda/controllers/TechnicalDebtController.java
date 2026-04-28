package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/technical-debt")
@RequiredArgsConstructor
public class TechnicalDebtController {

    private final TechnicalDebtService technicalDebtService;
    private final UserService userService;
    private final UserStoryService userStoryService;
    private final TasksService tasksService;
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> getByProject(@PathVariable Long projectId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long assignedToId) {
        if (projectService.findById(projectId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }
        List<TechnicalDebt> debts;
        if (status != null && !status.isBlank()) {
            debts = technicalDebtService.findByStatus(status).stream()
                    .filter(d -> d.getProject().getId().equals(projectId))
                    .toList();
        } else if (assignedToId != null) {
            debts = technicalDebtService.findByAssignedToId(assignedToId).stream()
                    .filter(d -> d.getProject().getId().equals(projectId))
                    .toList();
        } else {
            debts = technicalDebtService.findByProjectId(projectId);
        }
        return ResponseEntity.ok(debts);
    }

    @GetMapping("/{debtId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId, @PathVariable Long debtId) {
        return technicalDebtService.findById(debtId)
                .filter(d -> d.getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Technical debt not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId, @RequestBody Map<String, Object> payload) {
        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }
        String title = asString(payload.get("title"));
        String debtType = asString(payload.get("debtType"));
        Long assignedToId = asLong(payload.get("assignedToId"));
        Long createdById = asLong(payload.get("createdById"));
        if (title == null || title.isBlank() || debtType == null || debtType.isBlank() || assignedToId == null || createdById == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "title, debtType, assignedToId and createdById are required"));
        }
        User assignedTo = userService.findById(assignedToId).orElse(null);
        User createdBy = userService.findById(createdById).orElse(null);
        if (assignedTo == null || createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Assigned or createdBy user not found"));
        }

        TechnicalDebt debt = new TechnicalDebt();
        debt.setProject(project);
        debt.setTitle(title);
        debt.setDebtType(debtType);
        debt.setDescription(asString(payload.get("description")));
        debt.setPriority(defaulted(asString(payload.get("priority")), "medium"));
        debt.setStatus(defaulted(asString(payload.get("status")), "open"));
        debt.setAssignedTo(assignedTo);
        debt.setCreatedBy(createdBy);

        Long userStoryId = asLong(payload.get("userStoryId"));
        if (userStoryId != null) {
            UserStory story = userStoryService.findById(userStoryId).orElse(null);
            if (story == null || !story.getFeature().getCapability().getProject().getId().equals(projectId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found in project"));
            }
            debt.setUserStory(story);
        }
        Long taskId = asLong(payload.get("taskId"));
        if (taskId != null) {
            Tasks task = tasksService.findById(taskId).orElse(null);
            if (task == null || !task.getProject().getId().equals(projectId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Task not found in project"));
            }
            debt.setTask(task);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(technicalDebtService.save(debt));
    }

    @PatchMapping("/{debtId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long debtId,
                                   @RequestBody Map<String, Object> payload) {
        TechnicalDebt debt = technicalDebtService.findById(debtId)
                .filter(d -> d.getProject().getId().equals(projectId))
                .orElse(null);
        if (debt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Technical debt not found"));
        }
        if (payload.containsKey("title")) debt.setTitle(asString(payload.get("title")));
        if (payload.containsKey("description")) debt.setDescription(asString(payload.get("description")));
        if (payload.containsKey("debtType")) debt.setDebtType(asString(payload.get("debtType")));
        if (payload.containsKey("priority")) debt.setPriority(asString(payload.get("priority")));
        if (payload.containsKey("status")) debt.setStatus(asString(payload.get("status")));
        if (payload.containsKey("resolvedAt")) {
            String resolvedAt = asString(payload.get("resolvedAt"));
            debt.setResolvedAt(resolvedAt == null || resolvedAt.isBlank() ? null : LocalDateTime.parse(resolvedAt));
        }
        if (payload.containsKey("assignedToId")) {
            Long assignedToId = asLong(payload.get("assignedToId"));
            if (assignedToId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "assignedToId must be numeric"));
            }
            User assignedTo = userService.findById(assignedToId).orElse(null);
            if (assignedTo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Assigned user not found"));
            }
            debt.setAssignedTo(assignedTo);
        }
        if (payload.containsKey("updatedById")) {
            Long updatedById = asLong(payload.get("updatedById"));
            if (updatedById != null) {
                User updatedBy = userService.findById(updatedById).orElse(null);
                if (updatedBy == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
                }
                debt.setUpdatedBy(updatedBy);
            }
            debt.setUpdatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(technicalDebtService.save(debt));
    }

    @DeleteMapping("/{debtId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long debtId) {
        TechnicalDebt debt = technicalDebtService.findById(debtId)
                .filter(d -> d.getProject().getId().equals(projectId))
                .orElse(null);
        if (debt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Technical debt not found"));
        }
        technicalDebtService.deleteById(debtId);
        return ResponseEntity.noContent().build();
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

    private String defaulted(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
