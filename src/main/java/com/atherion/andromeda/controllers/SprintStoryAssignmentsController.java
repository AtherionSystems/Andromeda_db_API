package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateSprintStoryAssignmentRequest;
import com.atherion.andromeda.dto.UpdateSprintStoryAssignmentRequest;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.SprintStoryAssignmentService;
import com.atherion.andromeda.services.UserStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints/{sprintId}/user_stories")
@RequiredArgsConstructor
public class SprintStoryAssignmentsController {

    private final SprintStoryAssignmentService sprintStoryAssignmentService;
    private final SprintService sprintService;
    private final UserStoryService userStoryService;

    @GetMapping
    public ResponseEntity<?> getSprintStoryAssignments(@PathVariable Long projectId,
                                                       @PathVariable Long sprintId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }
        return ResponseEntity.ok(sprintStoryAssignmentService.findBySprintId(sprintId));
    }

    @GetMapping("/{sprintStoryAssignmentId}")
    public ResponseEntity<?> getSprintStoryAssignmentById(@PathVariable Long projectId,
                                                          @PathVariable Long sprintId,
                                                          @PathVariable Long sprintStoryAssignmentId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        return sprintStoryAssignmentService.findById(sprintStoryAssignmentId)
                .filter(assignment -> assignment.getSprint().getId().equals(sprintId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(notFound("Sprint story assignment not found"));
    }

    @PostMapping
    public ResponseEntity<?> createSprintStoryAssignment(@PathVariable Long projectId,
                                                         @PathVariable Long sprintId,
                                                         @Valid @RequestBody CreateSprintStoryAssignmentRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        UserStory userStory = findUserStoryInProject(projectId, request.userStoryId());
        if (userStory == null) {
            return notFound("User story not found");
        }

        if (sprintStoryAssignmentService.isStoryActiveInSprint(sprintId, request.userStoryId())) {
            return conflict("User story is already active in this sprint");
        }

        SprintStoryAssignment assignment = new SprintStoryAssignment();
        assignment.setSprint(sprint);
        assignment.setUserStoryId(request.userStoryId());
        assignment.setAddedAt(LocalDateTime.now());
        assignment.setIsActive(1);

        return ResponseEntity.status(HttpStatus.CREATED).body(sprintStoryAssignmentService.save(assignment));
    }

    @PatchMapping("/{sprintStoryAssignmentId}")
    public ResponseEntity<?> updateSprintStoryAssignment(@PathVariable Long projectId,
                                                         @PathVariable Long sprintId,
                                                         @PathVariable Long sprintStoryAssignmentId,
                                                         @RequestBody UpdateSprintStoryAssignmentRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        SprintStoryAssignment assignment = sprintStoryAssignmentService.findById(sprintStoryAssignmentId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (assignment == null) {
            return notFound("Sprint story assignment not found");
        }

        if (request.removedAt() != null) {
            try {
                assignment.setRemovedAt(LocalDateTime.parse(request.removedAt()));
                assignment.setIsActive(0);
            } catch (Exception e) {
                return badRequest("removedAt must be ISO-8601 LocalDateTime");
            }
        }

        if (request.movedToId() != null) {
            Sprint movedTo = findSprintInProject(projectId, request.movedToId());
            if (movedTo == null) {
                return notFound("Target sprint not found");
            }
            assignment.setMovedTo(movedTo);
        }

        return ResponseEntity.ok(sprintStoryAssignmentService.save(assignment));
    }

    @DeleteMapping("/{sprintStoryAssignmentId}")
    public ResponseEntity<?> deleteSprintStoryAssignment(@PathVariable Long projectId,
                                                         @PathVariable Long sprintId,
                                                         @PathVariable Long sprintStoryAssignmentId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        SprintStoryAssignment assignment = sprintStoryAssignmentService.findById(sprintStoryAssignmentId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (assignment == null) {
            return notFound("Sprint story assignment not found");
        }

        sprintStoryAssignmentService.deleteById(sprintStoryAssignmentId);
        return ResponseEntity.noContent().build();
    }

    private Sprint findSprintInProject(Long projectId, Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject() != null && s.getProject().getId().equals(projectId))
                .orElse(null);
    }

    private UserStory findUserStoryInProject(Long projectId, Long userStoryId) {
        return userStoryService.findById(userStoryId)
                .filter(story -> story.getFeature() != null
                        && story.getFeature().getCapability() != null
                        && story.getFeature().getCapability().getProject() != null
                        && story.getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
    }
}
