package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintsController {

    private final SprintService sprintService;
    private final ProjectService projectService;

    // GET /api/projects/{projectId}/sprints
    @GetMapping
    public ResponseEntity<?> getSprintsByProject(@PathVariable Long projectId) {
        if (projectService.findById(projectId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
        }
        return ResponseEntity.ok(sprintService.findByProjectId(projectId));
    }

    // GET /api/projects/{projectId}/sprints/{sprintId}
    @GetMapping("/{sprintId}")
    public ResponseEntity<?> getSprintById(@PathVariable Long projectId, @PathVariable Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(sprint -> sprint.getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Sprint not found")));
    }

    // POST /api/projects/{projectId}/sprints
    @PostMapping
    public ResponseEntity<?> createSprint(@PathVariable Long projectId, @RequestBody Sprint sprint) {
        if (sprint.getName() == null || sprint.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
        }

        sprint.setProject(project);
        if (sprint.getStatus() == null) {
            sprint.setStatus("planned");
        }

        Sprint saved = sprintService.save(sprint);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PATCH /api/projects/{projectId}/sprints/{sprintId}
    @PatchMapping("/{sprintId}")
    public ResponseEntity<?> updateSprint(@PathVariable Long projectId,
                                          @PathVariable Long sprintId,
                                          @RequestBody Sprint sprintDetails) {
        Sprint sprint = sprintService.findById(sprintId)
                .filter(existing -> existing.getProject().getId().equals(projectId))
                .orElse(null);

        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        if (sprintDetails.getName() != null) sprint.setName(sprintDetails.getName());
        if (sprintDetails.getGoal() != null) sprint.setGoal(sprintDetails.getGoal());
        if (sprintDetails.getStatus() != null) sprint.setStatus(sprintDetails.getStatus());
        if (sprintDetails.getStartDate() != null) sprint.setStartDate(sprintDetails.getStartDate());
        if (sprintDetails.getDueDate() != null) sprint.setDueDate(sprintDetails.getDueDate());
        if (sprintDetails.getActualEnd() != null) sprint.setActualEnd(sprintDetails.getActualEnd());

        return ResponseEntity.ok(sprintService.save(sprint));
    }

    // DELETE /api/projects/{projectId}/sprints/{sprintId}
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<?> deleteSprint(@PathVariable Long projectId, @PathVariable Long sprintId) {
        Sprint sprint = sprintService.findById(sprintId)
                .filter(existing -> existing.getProject().getId().equals(projectId))
                .orElse(null);

        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        sprintService.deleteById(sprintId);
        return ResponseEntity.noContent().build();
    }
}
