package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateUserStoryDependencyRequest;
import com.atherion.andromeda.dto.UpdateUserStoryDependencyRequest;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.model.UserStoryDependency;
import com.atherion.andromeda.services.UserStoryDependencyService;
import com.atherion.andromeda.services.UserStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/stories/{storyId}/dependencies")
@RequiredArgsConstructor
public class UserStoryDependenciesController {

    private final UserStoryService userStoryService;
    private final UserStoryDependencyService dependencyService;

    @GetMapping
    public ResponseEntity<?> getByStory(@PathVariable Long projectId, @PathVariable Long storyId) {
        UserStory story = findStoryInProject(projectId, storyId);
        if (story == null) {
            return notFound("User story not found");
        }
        return ResponseEntity.ok(dependencyService.findByStoryId(storyId));
    }

    @GetMapping("/{dependencyId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId,
                                     @PathVariable Long storyId,
                                     @PathVariable Long dependencyId) {
        return dependencyService.findById(dependencyId)
                .filter(d -> d.getStory().getId().equals(storyId)
                        && d.getStory().getFeature().getCapability().getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(notFound("Dependency not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long storyId,
                                    @Valid @RequestBody CreateUserStoryDependencyRequest request) {
        UserStory story = findStoryInProject(projectId, storyId);
        if (story == null) {
            return notFound("User story not found");
        }

        UserStory blockedBy = findStoryInProject(projectId, request.blockedById());
        if (blockedBy == null) {
            return notFound("blockedBy story not found");
        }

        UserStoryDependency dependency = new UserStoryDependency();
        dependency.setStory(story);
        dependency.setBlockedBy(blockedBy);
        dependency.setDependencyType(request.dependencyType() != null ? request.dependencyType() : "blocks");
        return ResponseEntity.status(HttpStatus.CREATED).body(dependencyService.save(dependency));
    }

    @PatchMapping("/{dependencyId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long storyId,
                                   @PathVariable Long dependencyId,
                                   @RequestBody UpdateUserStoryDependencyRequest request) {
        UserStoryDependency dependency = dependencyService.findById(dependencyId)
                .filter(d -> d.getStory().getId().equals(storyId)
                        && d.getStory().getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (dependency == null) {
            return notFound("Dependency not found");
        }
        if (request.dependencyType() != null) {
            dependency.setDependencyType(request.dependencyType());
        }
        return ResponseEntity.ok(dependencyService.save(dependency));
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable Long storyId,
                                    @PathVariable Long dependencyId) {
        UserStoryDependency dependency = dependencyService.findById(dependencyId)
                .filter(d -> d.getStory().getId().equals(storyId)
                        && d.getStory().getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (dependency == null) {
            return notFound("Dependency not found");
        }
        dependencyService.deleteById(dependencyId);
        return ResponseEntity.noContent().build();
    }

    private UserStory findStoryInProject(Long projectId, Long storyId) {
        return userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
    }
}
