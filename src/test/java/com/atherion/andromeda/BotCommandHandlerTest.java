package com.atherion.andromeda;

import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import com.atherion.andromeda.telegram.BotCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotCommandHandlerTest {

    @Mock private ProjectService projectService;
    @Mock private TasksService tasksService;
    @Mock private UserService userService;
    @Mock private ProjectMemberService projectMemberService;
    @Mock private SprintService sprintService;
    @Mock private SprintStoryAssignmentService sprintStoryAssignmentService;
    @Mock private TaskAssignmentService taskAssignmentService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    private BotCommandHandler handler;

    @BeforeEach
    void setup() {
        handler = new BotCommandHandler(
                projectService,
                tasksService,
                userService,
                projectMemberService,
                sprintService,
                sprintStoryAssignmentService,
                taskAssignmentService,
                passwordEncoder
        );
    }

    @Test
    void newSprint_createsSprintForProject() {
        User user = new User();
        user.setId(10L);
        user.setUsername("dev");

        Project project = new Project();
        project.setId(1L);
        project.setName("Andromeda");

        Sprint saved = new Sprint();
        saved.setId(3L);
        saved.setProject(project);
        saved.setName("Sprint 3");
        saved.setStatus("active");
        saved.setStartDate(java.time.LocalDateTime.of(2026, 4, 15, 0, 0));
        saved.setDueDate(java.time.LocalDateTime.of(2026, 4, 30, 0, 0));

        when(userService.findByTelegramId(123L)).thenReturn(Optional.of(user));
        when(projectService.findById(1L)).thenReturn(Optional.of(project));
        when(sprintService.save(any(Sprint.class))).thenReturn(saved);

        String response = handler.handle(
                "/newsprint 1 | Sprint 3 | Finish auth | active | 2026-04-15 | 2026-04-30",
                123L
        );

        ArgumentCaptor<Sprint> captor = ArgumentCaptor.forClass(Sprint.class);
        verify(sprintService).save(captor.capture());
        assertEquals("Sprint 3", captor.getValue().getName());
        assertEquals("active", captor.getValue().getStatus());
        assertTrue(response.contains("Sprint created!"));
    }

    @Test
    void addSprintTask_alias_assignsTaskToSprint() {
        User user = new User();
        user.setId(2L);
        user.setUsername("dev");

        Project project = new Project();
        project.setId(1L);
        project.setName("Andromeda");

        Sprint sprint = new Sprint();
        sprint.setId(5L);
        sprint.setName("Sprint 5");
        sprint.setProject(project);

        Tasks task = new Tasks();
        task.setId(9L);
        task.setTitle("Fix login");
        task.setStatus("todo");
        task.setProject(project);
        task.setUserStoryId(77L);

        when(userService.findByTelegramId(321L)).thenReturn(Optional.of(user));
        when(sprintService.findById(5L)).thenReturn(Optional.of(sprint));
        when(tasksService.findById(9L)).thenReturn(Optional.of(task));
        when(sprintStoryAssignmentService.isStoryActiveInSprint(5L, 77L)).thenReturn(false);
        when(taskAssignmentService.findByTaskIdAndUserId(9L, 2L)).thenReturn(Optional.empty());
        when(tasksService.save(any(Tasks.class))).thenReturn(task);

        String response = handler.handle("/addsprinttask 5 9", 321L);

        verify(sprintStoryAssignmentService).save(any(SprintStoryAssignment.class));
        verify(taskAssignmentService).save(any(TaskAssignment.class));
        assertTrue(response.contains("Task assigned to sprint!"));
    }
}
