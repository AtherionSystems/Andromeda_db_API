package com.atherion.andromeda.controllers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // Asumo que existe para validar al usuario
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.TaskAssignmentService;
import com.atherion.andromeda.services.TasksService;
import com.atherion.andromeda.services.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}/assignments")
@RequiredArgsConstructor
public class TaskAssignmentsController {

    private final TaskAssignmentService taskAssignmentService;
    private final TasksService tasksService;
    private final UserService userService;

    // GET /api/projects/{projectId}/tasks/{taskId}/assignments
    @GetMapping
    public ResponseEntity<List<TaskAssignment>> getAssignmentsByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskAssignmentService.findByTaskId(taskId));
    }

    // POST /api/projects/{projectId}/tasks/{taskId}/assignments
    @PostMapping
    public ResponseEntity<?> assignUserToTask(
            @PathVariable Long taskId,
            @RequestBody Map<String, Long> payload) {

        Long userId = payload.get("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }

        Tasks task = tasksService.findById(taskId).orElse(null);
        User user = userService.findById(userId).orElse(null);

        if (task == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Task or User not found"));
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUser(user);
        assignment.setAssignedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(taskAssignmentService.save(assignment));
    }

    // DELETE /api/projects/{projectId}/tasks/{taskId}/assignments/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeAssignment(@PathVariable Long taskId, @PathVariable Long userId) {
        Optional<TaskAssignment> assignment = taskAssignmentService.findByTaskIdAndUserId(taskId, userId);

        if (assignment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Assignment not found for this user and task"));
        }

        taskAssignmentService.deleteById(assignment.get().getId());
        return ResponseEntity.noContent().build();
    }
}