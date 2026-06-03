package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateSprintTaskRequest;
import com.atherion.andromeda.dto.SprintStoryAssignmentResponse;
import com.atherion.andromeda.dto.UpdateSprintTaskRequest;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.SprintStoryAssignmentService;
import com.atherion.andromeda.services.TasksService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping({
        "/api/projects/{projectId}/sprints/{sprintId}/tasks",
        "/api/projects/{projectId}/sprints/{sprintId}/sprint_tasks"
})
@RequiredArgsConstructor
public class SprintTasksController {

    private final SprintStoryAssignmentService sprintStoryAssignmentService;
    private final SprintService sprintService;
    private final TasksService tasksService;

    @GetMapping
    public ResponseEntity<?> getSprintTasks(@PathVariable Long projectId, @PathVariable Long sprintId) {
        if (findSprintInProject(projectId, sprintId) == null) {
            return notFound("Sprint not found");
        }
        return ResponseEntity.ok(sprintStoryAssignmentService.findBySprintIdAsResponse(sprintId));
    }

    @GetMapping("/{sprintTaskId}")
    public ResponseEntity<?> getSprintTaskById(@PathVariable Long projectId,
                                               @PathVariable Long sprintId,
                                               @PathVariable Long sprintTaskId) {
        if (findSprintInProject(projectId, sprintId) == null) {
            return notFound("Sprint not found");
        }
        return sprintStoryAssignmentService.findByIdAsResponse(sprintTaskId)
                .filter(st -> st.sprintId().equals(sprintId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(notFound("Sprint task not found"));
    }

    @PostMapping
    public ResponseEntity<?> createSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @Valid @RequestBody CreateSprintTaskRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        Tasks task = tasksService.findById(request.taskId()).orElse(null);
        if (task == null || !task.getProject().getId().equals(projectId)) {
            return notFound("Task not found");
        }

        if (task.getUserStoryId() == null) {
            return badRequest("Task is not linked to a user story");
        }

        if (sprintStoryAssignmentService.isStoryActiveInSprint(sprintId, task.getUserStoryId())) {
            return conflict("Task is already active in this sprint");
        }

        SprintStoryAssignment sprintTask = new SprintStoryAssignment();
        sprintTask.setSprint(sprint);
        sprintTask.setUserStoryId(task.getUserStoryId());
        sprintTask.setAddedAt(LocalDateTime.now());
        sprintTask.setIsActive(1);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SprintStoryAssignmentResponse.from(sprintStoryAssignmentService.save(sprintTask)));
    }

    @PatchMapping("/{sprintTaskId}")
    public ResponseEntity<?> updateSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @PathVariable Long sprintTaskId,
                                              @RequestBody UpdateSprintTaskRequest request) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return notFound("Sprint not found");
        }

        SprintStoryAssignment sprintTask = sprintStoryAssignmentService.findById(sprintTaskId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (sprintTask == null) {
            return notFound("Sprint task not found");
        }

        if (request.removedAt() != null) {
            try {
                sprintTask.setRemovedAt(LocalDateTime.parse(request.removedAt()));
                sprintTask.setIsActive(0);
            } catch (Exception e) {
                return badRequest("removedAt must be ISO-8601 LocalDateTime");
            }
        }

        if (request.movedToId() != null) {
            Sprint movedTo = findSprintInProject(projectId, request.movedToId());
            if (movedTo == null) {
                return notFound("Target sprint not found");
            }
            sprintTask.setMovedTo(movedTo);
        }

        return ResponseEntity.ok(SprintStoryAssignmentResponse.from(sprintStoryAssignmentService.save(sprintTask)));
    }

    @DeleteMapping("/{sprintTaskId}")
    public ResponseEntity<?> deleteSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @PathVariable Long sprintTaskId) {
        if (findSprintInProject(projectId, sprintId) == null) {
            return notFound("Sprint not found");
        }

        SprintStoryAssignment sprintTask = sprintStoryAssignmentService.findById(sprintTaskId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (sprintTask == null) {
            return notFound("Sprint task not found");
        }

        sprintStoryAssignmentService.deleteById(sprintTaskId);
        return ResponseEntity.noContent().build();
    }

    private Sprint findSprintInProject(Long projectId, Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
    }
}
