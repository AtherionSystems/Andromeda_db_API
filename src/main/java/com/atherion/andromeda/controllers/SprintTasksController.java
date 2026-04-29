package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.SprintStoryAssignmentService;
import com.atherion.andromeda.services.TasksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

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
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }
        return ResponseEntity.ok(sprintStoryAssignmentService.findBySprintId(sprintId));
    }

    @GetMapping("/{sprintTaskId}")
    public ResponseEntity<?> getSprintTaskById(@PathVariable Long projectId,
                                               @PathVariable Long sprintId,
                                               @PathVariable Long sprintTaskId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        return sprintStoryAssignmentService.findById(sprintTaskId)
                .filter(st -> st.getSprint().getId().equals(sprintId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Sprint task not found")));
    }

    @PostMapping
    public ResponseEntity<?> createSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @RequestBody Map<String, Long> payload) {
        Long taskId = payload.get("taskId");
        if (taskId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "taskId is required"));
        }

        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        Tasks task = tasksService.findById(taskId).orElse(null);
        if (task == null || !task.getProject().getId().equals(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task not found"));
        }

        if (task.getUserStoryId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Task is not linked to a user story"));
        }

        if (sprintStoryAssignmentService.isStoryActiveInSprint(sprintId, task.getUserStoryId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Task is already active in this sprint"));
        }

        SprintStoryAssignment sprintTask = new SprintStoryAssignment();
        sprintTask.setSprint(sprint);
        sprintTask.setUserStoryId(task.getUserStoryId());
        sprintTask.setAddedAt(LocalDateTime.now());
        sprintTask.setIsActive(1);

        return ResponseEntity.status(HttpStatus.CREATED).body(sprintStoryAssignmentService.save(sprintTask));
    }

    @PatchMapping("/{sprintTaskId}")
    public ResponseEntity<?> updateSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @PathVariable Long sprintTaskId,
                                              @RequestBody Map<String, Object> payload) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        SprintStoryAssignment sprintTask = sprintStoryAssignmentService.findById(sprintTaskId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (sprintTask == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint task not found"));
        }

        if (payload.containsKey("removedAt") && payload.get("removedAt") != null) {
            try {
                sprintTask.setRemovedAt(LocalDateTime.parse(payload.get("removedAt").toString()));
                sprintTask.setIsActive(0);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "removedAt must be ISO-8601 LocalDateTime"));
            }
        }

        if (payload.containsKey("movedToId") && payload.get("movedToId") != null) {
            Long movedToId = asLong(payload.get("movedToId"));
            if (movedToId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "movedToId must be numeric"));
            }
            Sprint movedTo = findSprintInProject(projectId, movedToId);
            if (movedTo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Target sprint not found"));
            }
            sprintTask.setMovedTo(movedTo);
        }

        return ResponseEntity.ok(sprintStoryAssignmentService.save(sprintTask));
    }

    @DeleteMapping("/{sprintTaskId}")
    public ResponseEntity<?> deleteSprintTask(@PathVariable Long projectId,
                                              @PathVariable Long sprintId,
                                              @PathVariable Long sprintTaskId) {
        Sprint sprint = findSprintInProject(projectId, sprintId);
        if (sprint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint not found"));
        }

        SprintStoryAssignment sprintTask = sprintStoryAssignmentService.findById(sprintTaskId)
                .filter(existing -> existing.getSprint().getId().equals(sprintId))
                .orElse(null);

        if (sprintTask == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sprint task not found"));
        }

        sprintStoryAssignmentService.deleteById(sprintTaskId);
        return ResponseEntity.noContent().build();
    }

    private Sprint findSprintInProject(Long projectId, Long sprintId) {
        return sprintService.findById(sprintId)
                .filter(s -> s.getProject().getId().equals(projectId))
                .orElse(null);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
