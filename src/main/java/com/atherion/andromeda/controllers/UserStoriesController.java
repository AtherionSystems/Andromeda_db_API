package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateUserStoryRequest;
import com.atherion.andromeda.dto.UpdateUserStoryRequest;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.FeatureService;
import com.atherion.andromeda.services.UserService;
import com.atherion.andromeda.services.UserStoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

import static com.atherion.andromeda.util.ControllerUtils.defaulted;

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
                                    @Valid @RequestBody CreateUserStoryRequest request) {
        Feature feature = findFeature(projectId, capabilityId, featureId);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found"));
        }

        User createdBy = userService.findById(request.createdById()).orElse(null);
        if (createdBy == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "createdBy user not found"));
        }

        UserStory story = new UserStory();
        story.setFeature(feature);
        story.setTitle(request.title());
        story.setDescription(request.description());
        story.setAcceptanceCriteria(request.acceptanceCriteria());
        story.setPriority(defaulted(request.priority(), "medium"));
        story.setStatus(defaulted(request.status(), "todo"));
        story.setStoryPoints(request.storyPoints());
        story.setCreatedBy(createdBy);

        if (request.ownerId() != null) {
            User owner = userService.findById(request.ownerId()).orElse(null);
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
                                   @RequestBody UpdateUserStoryRequest request) {
        UserStory story = userStoryService.findById(storyId)
                .filter(s -> s.getFeature().getId().equals(featureId)
                        && s.getFeature().getCapability().getId().equals(capabilityId)
                        && s.getFeature().getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (story == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User story not found"));
        }

        if (request.title() != null) story.setTitle(request.title());
        if (request.description() != null) story.setDescription(request.description());
        if (request.acceptanceCriteria() != null) story.setAcceptanceCriteria(request.acceptanceCriteria());
        if (request.priority() != null) story.setPriority(request.priority());
        if (request.status() != null) story.setStatus(request.status());
        if (request.storyPoints() != null) story.setStoryPoints(request.storyPoints());
        if (request.ownerId() != null) {
            User owner = userService.findById(request.ownerId()).orElse(null);
            if (owner == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "owner user not found"));
            }
            story.setOwner(owner);
        }
        if (request.updatedById() != null) {
            User updatedBy = userService.findById(request.updatedById()).orElse(null);
            if (updatedBy == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "updatedBy user not found"));
            }
            story.setUpdatedBy(updatedBy);
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
}
