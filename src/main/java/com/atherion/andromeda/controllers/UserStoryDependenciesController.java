package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.model.UserStoryDependency;
import com.atherion.andromeda.services.UserStoryDependencyService;
import com.atherion.andromeda.services.UserStoryService;
import lombok.RequiredArgsConstructor;
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found"));
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
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Dependency not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long storyId,
                                    @RequestBody Map<String, Object> payload) {
        UserStory story = findStoryInProject(projectId, storyId);
        if (story == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found"));
        }

        Long blockedById = asLong(payload.get("blockedById"));
        if (blockedById == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "blockedById is required"));
        }
        UserStory blockedBy = findStoryInProject(projectId, blockedById);
        if (blockedBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "blockedBy story not found"));
        }

        UserStoryDependency dependency = new UserStoryDependency();
        dependency.setStory(story);
        dependency.setBlockedBy(blockedBy);
        dependency.setDependencyType(payload.get("dependencyType") == null ? "blocks" : payload.get("dependencyType").toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(dependencyService.save(dependency));
    }

    @PatchMapping("/{dependencyId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long storyId,
                                   @PathVariable Long dependencyId,
                                   @RequestBody Map<String, Object> payload) {
        UserStoryDependency dependency = dependencyService.findById(dependencyId)
                .filter(d -> d.getStory().getId().equals(storyId)
                        && d.getStory().getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (dependency == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Dependency not found"));
        }
        if (payload.containsKey("dependencyType")) {
            dependency.setDependencyType(payload.get("dependencyType").toString());
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Dependency not found"));
        }
        dependencyService.deleteById(dependencyId);
        return ResponseEntity.noContent().build();
    }

    private UserStory findStoryInProject(Long projectId, Long storyId) {
        return userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getCapability().getProject().getId().equals(projectId))
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
}
