package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.ProjectResponse;
import com.atherion.andromeda.dto.TaskResponse;
import com.atherion.andromeda.dto.dashboard.MeDashboardResponse;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.ProjectMemberRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.atherion.andromeda.services.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TasksRepository tasksRepository;
    private final KpiService kpiService;

    @GetMapping("/projects")
    public ResponseEntity<?> getMyProjects(@AuthenticationPrincipal Object principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ProjectResponse> projects = projectMemberRepository.findProjectsByUserId(user.getId())
                .stream()
                .map(ProjectResponse::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getMyTasks(
            @AuthenticationPrincipal Object principal,
            @RequestParam Optional<Long> projectId,
            @RequestParam Optional<String> status) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<TaskResponse> tasks = tasksRepository.findByAssignedUserIdAsResponse(
                user.getId(),
                projectId.orElse(null),
                status.orElse(null));
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @AuthenticationPrincipal Object principal,
            @RequestParam Long projectId) throws Exception {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var distFuture  = kpiService.getMyTaskDistribution(user.getId(), projectId);
        var hoursFuture = kpiService.getMyHoursPerSprint(user.getId(), projectId);
        var tasksFuture = kpiService.getMyTasksPerSprint(user.getId(), projectId);

        java.util.concurrent.CompletableFuture.allOf(distFuture, hoursFuture, tasksFuture).join();

        return ResponseEntity.ok(
                MeDashboardResponse.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .projectId(projectId)
                        .generatedAt(LocalDateTime.now())
                        .myTaskDistribution(distFuture.get())
                        .myHoursPerSprint(hoursFuture.get())
                        .myTasksPerSprint(tasksFuture.get())
                        .build()
        );
    }

    private User resolveUser(Object principal) {
        if (principal instanceof Jwt jwt) {
            return userRepository.findByIamSub(jwt.getSubject()).orElse(null);
        }
        if (principal instanceof String username) {
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }
}
