package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.AssignUserToTaskRequest;
import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.TaskAssignmentService;
import com.atherion.andromeda.services.TasksService;
import com.atherion.andromeda.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}/assignments")
@RequiredArgsConstructor
public class TaskAssignmentsController {

    private final TaskAssignmentService taskAssignmentService;
    private final TasksService tasksService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<TaskAssignment>> getAssignmentsByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskAssignmentService.findByTaskId(taskId));
    }

    @PostMapping
    public ResponseEntity<?> assignUserToTask(@PathVariable Long taskId,
                                              @Valid @RequestBody AssignUserToTaskRequest request) {
        Tasks task = tasksService.findById(taskId).orElse(null);
        User user = userService.findById(request.userId()).orElse(null);

        if (task == null || user == null) {
            return notFound("Task or User not found");
        }

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUser(user);
        assignment.setAssignedAt(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED).body(taskAssignmentService.save(assignment));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> removeAssignment(@PathVariable Long taskId, @PathVariable Long userId) {
        Optional<TaskAssignment> assignment = taskAssignmentService.findByTaskIdAndUserId(taskId, userId);

        if (assignment.isEmpty()) {
            return notFound("Assignment not found for this user and task");
        }

        taskAssignmentService.deleteById(assignment.get().getId());
        return ResponseEntity.noContent().build();
    }
}
