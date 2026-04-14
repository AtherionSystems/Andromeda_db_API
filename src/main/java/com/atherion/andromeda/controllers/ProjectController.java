package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateProjectRequest;
import com.atherion.andromeda.dto.ProjectResponse;
import com.atherion.andromeda.dto.UpdateProjectRequest;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/projects", "/projects"})
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // GET /api/projects
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {
        List<ProjectResponse> projects = projectService.findAll().stream()
                .map(ProjectResponse::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return projectService.findById(id)
                .<ResponseEntity<?>>map(project -> ResponseEntity.ok(ProjectResponse.from(project)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Project not found")));
    }

    // POST /api/projects
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status() == null ? "active" : request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        Project saved = projectService.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(saved));
    }

    // PATCH /api/projects/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id,
                                   @Valid @RequestBody UpdateProjectRequest request) {
        Project project = projectService.findById(id).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
        }

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            project.setEndDate(request.endDate());
        }

        return ResponseEntity.ok(ProjectResponse.from(projectService.save(project)));
    }

    // DELETE api/projects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (projectService.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
        }
        projectService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
