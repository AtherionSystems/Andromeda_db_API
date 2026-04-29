package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.FeatureService;
import com.atherion.andromeda.services.UserService;
import com.atherion.andromeda.services.UserStoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories")
@RequiredArgsConstructor
public class UserStoriesController {

    private final UserStoryService userStoryService;
    private final FeatureService featureService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getByFeature(@PathVariable Long projectId,
                                          @PathVariable Long capabilityId,
                                          @PathVariable Long featureId) {
        Feature feature = findFeature(projectId, capabilityId, featureId);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found"));
        }
        return ResponseEntity.ok(userStoryService.findByFeatureId(featureId));
    }

    @GetMapping("/{storyId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId,
                                     @PathVariable Long capabilityId,
                                     @PathVariable Long featureId,
                                     @PathVariable Long storyId) {
        return userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getId().equals(featureId)
                        && s.getFeature().getCapability().getId().equals(capabilityId)
                        && s.getFeature().getCapability().getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long capabilityId,
                                    @PathVariable Long featureId,
                                    @RequestBody Map<String, Object> payload) {
        Feature feature = findFeature(projectId, capabilityId, featureId);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found"));
        }

        String title = asString(payload.get("title"));
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
        }

        Long createdById = asLong(payload.get("createdById"));
        if (createdById == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "createdById is required"));
        }
        User createdBy = userService.findById(createdById).orElse(null);
        if (createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "createdBy user not found"));
        }

        UserStory story = new UserStory();
        story.setFeature(feature);
        story.setTitle(title);
        story.setDescription(asString(payload.get("description")));
        story.setAcceptanceCriteria(asString(payload.get("acceptanceCriteria")));
        story.setPriority(defaulted(asString(payload.get("priority")), "medium"));
        story.setStatus(defaulted(asString(payload.get("status")), "todo"));
        story.setStoryPoints(asInteger(payload.get("storyPoints")));
        story.setCreatedBy(createdBy);

        Long ownerId = asLong(payload.get("ownerId"));
        if (ownerId != null) {
            User owner = userService.findById(ownerId).orElse(null);
            if (owner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "owner user not found"));
            }
            story.setOwner(owner);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userStoryService.save(story));
    }

    @PatchMapping("/{storyId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long capabilityId,
                                   @PathVariable Long featureId,
                                   @PathVariable Long storyId,
                                   @RequestBody Map<String, Object> payload) {
        UserStory story = userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getId().equals(featureId)
                        && s.getFeature().getCapability().getId().equals(capabilityId)
                        && s.getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (story == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found"));
        }

        if (payload.containsKey("title")) story.setTitle(asString(payload.get("title")));
        if (payload.containsKey("description")) story.setDescription(asString(payload.get("description")));
        if (payload.containsKey("acceptanceCriteria")) story.setAcceptanceCriteria(asString(payload.get("acceptanceCriteria")));
        if (payload.containsKey("priority")) story.setPriority(asString(payload.get("priority")));
        if (payload.containsKey("status")) story.setStatus(asString(payload.get("status")));
        if (payload.containsKey("storyPoints")) story.setStoryPoints(asInteger(payload.get("storyPoints")));
        if (payload.containsKey("ownerId")) {
            Long ownerId = asLong(payload.get("ownerId"));
            if (ownerId == null) {
                story.setOwner(null);
            } else {
                User owner = userService.findById(ownerId).orElse(null);
                if (owner == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "owner user not found"));
                }
                story.setOwner(owner);
            }
        }
        if (payload.containsKey("updatedById")) {
            Long updatedById = asLong(payload.get("updatedById"));
            if (updatedById != null) {
                User updatedBy = userService.findById(updatedById).orElse(null);
                if (updatedBy == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
                }
                story.setUpdatedBy(updatedBy);
            }
            story.setUpdatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(userStoryService.save(story));
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable Long capabilityId,
                                    @PathVariable Long featureId,
                                    @PathVariable Long storyId) {
        UserStory story = userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getId().equals(featureId)
                        && s.getFeature().getCapability().getId().equals(capabilityId)
                        && s.getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (story == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found"));
        }
        userStoryService.deleteById(storyId);
        return ResponseEntity.noContent().build();
    }

    private Feature findFeature(Long projectId, Long capabilityId, Long featureId) {
        return featureService.findById(featureId)
                .filter(f -> f.getCapability().getId().equals(capabilityId)
                        && f.getCapability().getProject().getId().equals(projectId))
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

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(value.toString());
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
