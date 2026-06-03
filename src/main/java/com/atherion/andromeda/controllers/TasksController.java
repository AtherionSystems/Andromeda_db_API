package com.atherion.andromeda.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atherion.andromeda.dto.TaskResponse;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.TasksService;

import lombok.RequiredArgsConstructor;
import static com.atherion.andromeda.util.ControllerUtils.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TasksController {

    private final TasksService tasksService;
    private final ProjectService projectService;

    // GET /api/projects/{projectId}/tasks
    // GET /api/projects/{projectId}/tasks?userStoryId={id}
    // GET /api/projects/{projectId}/tasks?status={status}
    // GET /api/projects/{projectId}/tasks?assignedTo={userId}
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestParam Optional<Long> userStoryId,
            @RequestParam Optional<String> status,
            @RequestParam Optional<Long> assignedTo) {
        List<TaskResponse> tasks;
        if (userStoryId.isPresent()) {
            tasks = tasksService.findByProjectIdAndUserStoryIdAsResponse(projectId, userStoryId.get());
        } else if (status.isPresent()) {
            tasks = tasksService.findByProjectIdAndStatusAsResponse(projectId, status.get());
        } else if (assignedTo.isPresent()) {
            tasks = tasksService.findByProjectIdAndAssignedUserIdAsResponse(projectId, assignedTo.get());
        } else {
            tasks = tasksService.findByProjectIdAsResponse(projectId);
        }
        return ResponseEntity.ok(tasks);
    }

    // GET /api/projects/{projectId}/tasks/{taskId}
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        return tasksService.findById(taskId)
                .<ResponseEntity<?>>map(task -> ResponseEntity.ok(task))
                .orElse(notFound("Task not found"));
    }

    // POST /api/projects/{projectId}/tasks
    @PostMapping
    public ResponseEntity<?> createTask(@PathVariable Long projectId, @RequestBody Tasks task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            return badRequest("title is required");
        }

        Project project = projectService.findById(projectId).orElse(null);

        if (project == null) {
            return notFound("Project not found");
        }

        task.setProject(project);
        if (task.getPriority() == null) task.setPriority("medium");
        if (task.getStatus() == null) task.setStatus("todo");

        Tasks saved = tasksService.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PATCH /api/projects/{projectId}/tasks/{taskId}
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Long taskId, @RequestBody Tasks taskDetails) {
        Tasks task = tasksService.findById(taskId).orElse(null);

        if (task == null) {
            return notFound("Task not found");
        }

        if (taskDetails.getTitle() != null) task.setTitle(taskDetails.getTitle());
        if (taskDetails.getDescription() != null) task.setDescription(taskDetails.getDescription());
        if (taskDetails.getStatus() != null) task.setStatus(taskDetails.getStatus());
        if (taskDetails.getPriority() != null) task.setPriority(taskDetails.getPriority());
        if (taskDetails.getStartDate() != null) task.setStartDate(taskDetails.getStartDate());
        if (taskDetails.getDueDate() != null) task.setDueDate(taskDetails.getDueDate());
        task.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(tasksService.save(task));
    }

    // DELETE /api/projects/{projectId}/tasks/{taskId}
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        if (tasksService.findById(taskId).isEmpty()) {
            return notFound("Task not found");
        }
        tasksService.deleteById(taskId);
        return ResponseEntity.noContent().build();
    }
}
