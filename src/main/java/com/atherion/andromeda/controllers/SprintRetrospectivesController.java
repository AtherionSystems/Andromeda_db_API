package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateSprintRetrospectiveRequest;
import com.atherion.andromeda.dto.UpdateSprintRetrospectiveRequest;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintRetrospective;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.SprintRetrospectiveService;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints/{sprintId}/retrospective")
@RequiredArgsConstructor
public class SprintRetrospectivesController {

    private final SprintRetrospectiveService sprintRetrospectiveService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getBySprint(@PathVariable Long projectId, @PathVariable Long sprintId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }
        return sprintRetrospectiveService.findBySprintId(sprintId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(notFound("Sprint retrospective not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @PathVariable Long sprintId,
                                    @Valid @RequestBody CreateSprintRetrospectiveRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }
        if (sprintRetrospectiveService.findBySprintId(sprintId).isPresent()) {
            return conflict("Sprint already has a retrospective");
        }

        User createdBy = userService.findById(request.createdById()).orElse(null);
        if (createdBy == null) {
            return notFound("createdBy user not found");
        }

        SprintRetrospective retrospective = new SprintRetrospective();
        retrospective.setSprint(sprint);
        retrospective.setSummary(request.summary());
        retrospective.setWhatWentWell(request.whatWentWell());
        retrospective.setWhatWentWrong(request.whatWentWrong());
        retrospective.setCreatedBy(createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintRetrospectiveService.save(retrospective));
    }

    @PatchMapping
    public ResponseEntity<?> patch(@PathVariable Long projectId,
                                   @PathVariable Long sprintId,
                                   @RequestBody UpdateSprintRetrospectiveRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }
        SprintRetrospective retrospective = sprintRetrospectiveService.findBySprintId(sprintId).orElse(null);
        if (retrospective == null) {
            return notFound("Sprint retrospective not found");
        }

        if (request.summary() != null) retrospective.setSummary(request.summary());
        if (request.whatWentWell() != null) retrospective.setWhatWentWell(request.whatWentWell());
        if (request.whatWentWrong() != null) retrospective.setWhatWentWrong(request.whatWentWrong());
        if (request.updatedById() != null) {
            User updatedBy = userService.findById(request.updatedById()).orElse(null);
            if (updatedBy == null) {
                return notFound("updatedBy user not found");
            }
            retrospective.setUpdatedBy(updatedBy);
            retrospective.setUpdatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(sprintRetrospectiveService.save(retrospective));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long sprintId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }
        SprintRetrospective retrospective = sprintRetrospectiveService.findBySprintId(sprintId).orElse(null);
        if (retrospective == null) {
            return notFound("Sprint retrospective not found");
        }
        sprintRetrospectiveService.deleteById(retrospective.getId());
        return ResponseEntity.noContent().build();
    }

    private Sprint findSprintInProject(Long projectId, Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
    }
}
