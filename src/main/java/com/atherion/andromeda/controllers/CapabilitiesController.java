package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.CapabilityService;
import com.atherion.andromeda.services.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/capabilities")
@RequiredArgsConstructor
public class CapabilitiesController {

    private final CapabilityService capabilityService;
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<?> getByProject(@PathVariable Long projectId) {
        if (projectService.findById(projectId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }
        return ResponseEntity.ok(capabilityService.findByProjectId(projectId));
    }

    @GetMapping("/{capabilityId}")
    public ResponseEntity<?> getById(@PathVariable Long projectId, @PathVariable Long capabilityId) {
        return capabilityService.findById(capabilityId)
                .filter(c -> c.getProject().getId().equals(projectId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Capability not found")));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId, @RequestBody Capability capability) {
        if (capability.getName() == null || capability.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }
        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }
        capability.setProject(project);
        if (capability.getStatus() == null) {
            capability.setStatus("active");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(capabilityService.save(capability));
    }

    @PatchMapping("/{capabilityId}")
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long capabilityId,
                                   @RequestBody Capability changes) {
        Capability capability = capabilityService.findById(capabilityId)
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElse(null);
        if (capability == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Capability not found"));
        }
        if (changes.getName() != null) capability.setName(changes.getName());
        if (changes.getDescription() != null) capability.setDescription(changes.getDescription());
        if (changes.getStatus() != null) capability.setStatus(changes.getStatus());
        capability.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(capabilityService.save(capability));
    }

    @DeleteMapping("/{capabilityId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long capabilityId) {
        Capability capability = capabilityService.findById(capabilityId)
                .filter(c -> c.getProject().getId().equals(projectId))
                .orElse(null);
        if (capability == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Capability not found"));
        }
        capabilityService.deleteById(capabilityId);
        return ResponseEntity.noContent().build();
    }
}
