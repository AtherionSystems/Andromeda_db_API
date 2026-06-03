package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.SprintResponse;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.services.ProjectMemberService;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static com.atherion.andromeda.util.ControllerUtils.*;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintsController {

    private final SprintService sprintService;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    @GetMapping
    public ResponseEntity<?> getSprintsByProject(@PathVariable Long projectId) {
        if (projectService.findById(projectId).isEmpty()) return notFound("Project not found");
        return ResponseEntity.ok(sprintService.findByProjectId(projectId));
    }

    @GetMapping("/{sprintId}")
    public ResponseEntity<?> getSprintById(@PathVariable Long projectId, @PathVariable Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(notFound("Sprint not found"));
    }

    @PostMapping
    public ResponseEntity<?> createSprint(@PathVariable Long projectId,
                                          @RequestBody Sprint sprint,
                                          @AuthenticationPrincipal Jwt jwt) {
        if (sprint.getName() == null || sprint.getName().isBlank()) return badRequest("name is required");

        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) return notFound("Project not found");

        String iamSub = jwt.getSubject();
        if (!projectMemberService.isManagerOrOwnerByIamSub(projectId, iamSub))
            return forbidden("Only managers and owners can create sprints");

        sprint.setProject(project);
        if (sprint.getStatus() == null) sprint.setStatus("planned");

        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.save(sprint));
    }

    @PatchMapping("/{sprintId}")
    public ResponseEntity<?> updateSprint(@PathVariable Long projectId,
                                          @PathVariable Long sprintId,
                                          @RequestBody Sprint sprintDetails,
                                          @AuthenticationPrincipal Jwt jwt) {
        Sprint sprint = sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
        if (sprint == null) return notFound("Sprint not found");

        String iamSub = jwt.getSubject();
        if (!projectMemberService.isManagerOrOwnerByIamSub(projectId, iamSub))
            return forbidden("Only managers and owners can update sprints");

        if (sprintDetails.getName() != null) sprint.setName(sprintDetails.getName());
        if (sprintDetails.getGoal() != null) sprint.setGoal(sprintDetails.getGoal());
        if (sprintDetails.getStatus() != null) sprint.setStatus(sprintDetails.getStatus());
        if (sprintDetails.getStartDate() != null) sprint.setStartDate(sprintDetails.getStartDate());
        if (sprintDetails.getDueDate() != null) sprint.setDueDate(sprintDetails.getDueDate());
        if (sprintDetails.getActualEnd() != null) sprint.setActualEnd(sprintDetails.getActualEnd());
        sprint.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(SprintResponse.from(sprintService.save(sprint)));
    }

    @DeleteMapping("/{sprintId}")
    public ResponseEntity<?> deleteSprint(@PathVariable Long projectId,
                                          @PathVariable Long sprintId,
                                          @AuthenticationPrincipal Jwt jwt) {
        Sprint sprint = sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
        if (sprint == null) return notFound("Sprint not found");

        String iamSub = jwt.getSubject();
        if (!projectMemberService.isManagerOrOwnerByIamSub(projectId, iamSub))
            return forbidden("Only managers and owners can delete sprints");

        sprintService.deleteById(sprintId);
        return ResponseEntity.noContent().build();
    }
}