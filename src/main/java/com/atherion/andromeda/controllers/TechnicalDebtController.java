package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateTechnicalDebtRequest;
import com.atherion.andromeda.dto.UpdateTechnicalDebtRequest;
import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.atherion.andromeda.util.ControllerUtils.defaulted;

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
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @Valid @RequestBody CreateTechnicalDebtRequest request) {
        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }

        User assignedTo = userService.findById(request.assignedToId()).orElse(null);
        User createdBy = userService.findById(request.createdById()).orElse(null);
        if (assignedTo == null || createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Assigned or createdBy user not found"));
        }

        TechnicalDebt debt = new TechnicalDebt();
        debt.setProject(project);
        debt.setTitle(request.title());
        debt.setDebtType(request.debtType());
        debt.setDescription(request.description());
        debt.setPriority(defaulted(request.priority(), "medium"));
        debt.setStatus(defaulted(request.status(), "open"));
        debt.setAssignedTo(assignedTo);
        debt.setCreatedBy(createdBy);

        if (request.userStoryId() != null) {
            UserStory story = userStoryService.findById(request.userStoryId()).orElse(null);
            if (story == null || !story.getFeature().getCapability().getProject().getId().equals(projectId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found in project"));
            }
            debt.setUserStory(story);
        }
        if (request.taskId() != null) {
            Tasks task = tasksService.findById(request.taskId()).orElse(null);
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
                                   @RequestBody UpdateTechnicalDebtRequest request) {
        TechnicalDebt debt = technicalDebtService.findById(debtId)
                .filter(d -> d.getProject().getId().equals(projectId))
                .orElse(null);
        if (debt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Technical debt not found"));
        }

        if (request.title() != null) debt.setTitle(request.title());
        if (request.description() != null) debt.setDescription(request.description());
        if (request.debtType() != null) debt.setDebtType(request.debtType());
        if (request.priority() != null) debt.setPriority(request.priority());
        if (request.status() != null) debt.setStatus(request.status());
        if (request.resolvedAt() != null) {
            debt.setResolvedAt(request.resolvedAt().isBlank() ? null : LocalDateTime.parse(request.resolvedAt()));
        }
        if (request.assignedToId() != null) {
            User assignedTo = userService.findById(request.assignedToId()).orElse(null);
            if (assignedTo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Assigned user not found"));
            }
            debt.setAssignedTo(assignedTo);
        }
        if (request.updatedById() != null) {
            User updatedBy = userService.findById(request.updatedById()).orElse(null);
            if (updatedBy == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
            }
            debt.setUpdatedBy(updatedBy);
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
}
