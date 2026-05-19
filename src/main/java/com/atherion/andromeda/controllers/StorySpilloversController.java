package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateStorySpilloverRequest;
import com.atherion.andromeda.dto.UpdateStorySpilloverRequest;
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

@RestController
@RequestMapping("/api/projects/{projectId}/story-spillovers")
@RequiredArgsConstructor
public class StorySpilloversController {

    private final StorySpilloverService spilloverService;
    private final SprintStoryAssignmentService sprintStoryAssignmentService;
    private final UserStoryService userStoryService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll(@PathVariable Long projectId,
                                    @RequestParam(required = false) Long userStoryId,
                                    @RequestParam(required = false) Long originSprintId,
                                    @RequestParam(required = false) Long destinationSprintId) {
        List<StorySpillover> list;
        if (userStoryId != null) {
            list = spilloverService.findByUserStoryId(userStoryId);
        } else if (originSprintId != null) {
            list = spilloverService.findByOriginSprintId(originSprintId);
        } else if (destinationSprintId != null) {
            list = spilloverService.findByDestinationSprintId(destinationSprintId);
        } else {
            list = spilloverService.findAll();
        }
        List<StorySpillover> filtered = list.stream()
                .filter(s -> s.getOriginSprint().getProject().getId().equals(projectId))
                .toList();
        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{spilloverId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId, @PathVariable Long spilloverId) {
        return spilloverService.findById(spilloverId)
                .filter(s -> s.getOriginSprint().getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Story spillover not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @Valid @RequestBody CreateStorySpilloverRequest request) {
        SprintStoryAssignment sprintStory = sprintStoryAssignmentService.findById(request.sprintStoryId()).orElse(null);
        UserStory userStory = userStoryService.findById(request.userStoryId()).orElse(null);
        Sprint origin = sprintService.findById(request.originSprintId()).orElse(null);
        Sprint destination = sprintService.findById(request.destinationSprintId()).orElse(null);
        User createdBy = userService.findById(request.createdById()).orElse(null);

        if (sprintStory == null || userStory == null || origin == null || destination == null || createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Related entity not found"));
        }
        if (!origin.getProject().getId().equals(projectId) || !destination.getProject().getId().equals(projectId)
                || !userStory.getFeature().getCapability().getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Entity does not belong to project"));
        }

        StorySpillover spillover = new StorySpillover();
        spillover.setSprintStory(sprintStory);
        spillover.setUserStory(userStory);
        spillover.setOriginSprint(origin);
        spillover.setDestinationSprint(destination);
        spillover.setReason(request.reason());
        spillover.setDetail(request.detail());
        spillover.setCreatedBy(createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(spilloverService.save(spillover));
    }

    @PatchMapping("/{spilloverId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long spilloverId,
                                   @RequestBody UpdateStorySpilloverRequest request) {
        StorySpillover spillover = spilloverService.findById(spilloverId)
                .filter(s -> s.getOriginSprint().getProject().getId().equals(projectId))
                .orElse(null);
        if (spillover == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Story spillover not found"));
        }

        if (request.reason() != null) spillover.setReason(request.reason());
        if (request.detail() != null) spillover.setDetail(request.detail());
        if (request.updatedById() != null) {
            User updatedBy = userService.findById(request.updatedById()).orElse(null);
            if (updatedBy == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
            }
            spillover.setUpdatedBy(updatedBy);
            spillover.setUpdatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(spilloverService.save(spillover));
    }

    @DeleteMapping("/{spilloverId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long spilloverId) {
        StorySpillover spillover = spilloverService.findById(spilloverId)
                .filter(s -> s.getOriginSprint().getProject().getId().equals(projectId))
                .orElse(null);
        if (spillover == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Story spillover not found"));
        }
        spilloverService.deleteById(spilloverId);
        return ResponseEntity.noContent().build();
    }
}
