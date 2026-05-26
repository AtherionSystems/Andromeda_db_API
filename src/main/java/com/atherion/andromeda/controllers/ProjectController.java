package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateProjectRequest;
import com.atherion.andromeda.dto.ProjectResponse;
import com.atherion.andromeda.dto.UpdateProjectRequest;
import com.atherion.andromeda.dto.UserStoryResponse;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.UserStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/api/projects", "/projects"})
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserStoryService userStoryService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAll() {
        List<ProjectResponse> projects = projectService.findAll().stream()
                .map(ProjectResponse::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}/stories")
    public ResponseEntity<List<UserStoryResponse>> getStoriesByProject(@PathVariable Long id) {
        return ResponseEntity.ok(userStoryService.findByProjectIdAsResponse(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return projectService.findById(id)
                .<ResponseEntity<?>>map(project -> ResponseEntity.ok(ProjectResponse.from(project)))
                .orElse(notFound("Project not found"));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(Optional.ofNullable(request.status()).orElse("active"));
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(projectService.save(project)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable Long id,
                                   @Valid @RequestBody UpdateProjectRequest request) {
        Optional<Project> projectOpt = projectService.findById(id);
        if (projectOpt.isEmpty()) {
            return notFound("Project not found");
        }

        Project project = projectOpt.get();
        applyPatch(project, request);
        return ResponseEntity.ok(ProjectResponse.from(projectService.save(project)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (projectService.findById(id).isEmpty()) {
            return notFound("Project not found");
        }
        projectService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyPatch(Project project, UpdateProjectRequest request) {
        Optional.ofNullable(request.name()).ifPresent(project::setName);
        Optional.ofNullable(request.description()).ifPresent(project::setDescription);
        Optional.ofNullable(request.status()).ifPresent(project::setStatus);
        Optional.ofNullable(request.startDate()).ifPresent(project::setStartDate);
        Optional.ofNullable(request.endDate()).ifPresent(project::setEndDate);
        project.setUpdatedAt(LocalDateTime.now());
    }
}
