package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.services.CapabilityService;
import com.atherion.andromeda.services.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/capabilities/{capabilityId}/features")
@RequiredArgsConstructor
public class FeaturesController {

    private final FeatureService featureService;
    private final CapabilityService capabilityService;

    @GetMapping
    public ResponseEntity<?> getByCapability(@PathVariable Long projectId, @PathVariable Long capabilityId) {
        Capability capability = capabilityService.findById(capabilityId)
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElse(null);
        if (capability == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Capability not found"));
        }
        return ResponseEntity.ok(featureService.findByCapabilityId(capabilityId));
    }

    @GetMapping("/{featureId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId,
                                     @PathVariable Long capabilityId,
                                     @PathVariable Long featureId) {
        return featureService.findById(featureId)
                .filter(f -> f.getCapability().getId().equals(capabilityId)
                        && f.getCapability().getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long capabilityId,
                                    @RequestBody Feature feature) {
        if (feature.getName() == null || feature.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }
        Capability capability = capabilityService.findById(capabilityId)
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElse(null);
        if (capability == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Capability not found"));
        }
        feature.setCapability(capability);
        if (feature.getStatus() == null) {
            feature.setStatus("active");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(featureService.save(feature));
    }

    @PatchMapping("/{featureId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long capabilityId,
                                   @PathVariable Long featureId,
                                   @RequestBody Feature changes) {
        Feature feature = featureService.findById(featureId)
                .filter(f -> f.getCapability().getId().equals(capabilityId)
                        && f.getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found"));
        }
        if (changes.getName() != null) feature.setName(changes.getName());
        if (changes.getDescription() != null) feature.setDescription(changes.getDescription());
        if (changes.getStatus() != null) feature.setStatus(changes.getStatus());
        feature.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(featureService.save(feature));
    }

    @DeleteMapping("/{featureId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId,
                                    @PathVariable Long capabilityId,
                                    @PathVariable Long featureId) {
        Feature feature = featureService.findById(featureId)
                .filter(f -> f.getCapability().getId().equals(capabilityId)
                        && f.getCapability().getProject().getId().equals(projectId))
                .orElse(null);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Feature not found"));
        }
        featureService.deleteById(featureId);
        return ResponseEntity.noContent().build();
    }
}
