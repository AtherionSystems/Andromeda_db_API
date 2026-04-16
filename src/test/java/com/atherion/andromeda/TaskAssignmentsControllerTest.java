package com.atherion.andromeda;

import com.atherion.andromeda.controllers.TaskAssignmentsController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.TaskAssignmentService;
import com.atherion.andromeda.services.TasksService;
import com.atherion.andromeda.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentsControllerTest {

    @Mock private TaskAssignmentService taskAssignmentService;
    @Mock private TasksService tasksService;
    @Mock private UserService userService;

    @InjectMocks private TaskAssignmentsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Project buildProject(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setName("Project");
        p.setStatus("active");
        return p;
    }

    private Tasks buildTask(Long id) {
        Tasks t = new Tasks();
        t.setId(id);
        t.setTitle("Task");
        t.setPriority("medium");
        t.setStatus("todo");
        t.setProject(buildProject(1L));
        return t;
    }

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setName("User");
        u.setUsername("user" + id);
        u.setEmail("user" + id + "@example.com");
        u.setPasswordHash("hashed");
        return u;
    }

    private TaskAssignment buildAssignment(Long id, Tasks task, User user) {
        TaskAssignment a = new TaskAssignment();
        a.setId(id);
        a.setTask(task);
        a.setUser(user);
        a.setAssignedAt(LocalDateTime.now());
        return a;
    }

    @Test
    void getAssignmentsByTask_returns200WithList() throws Exception {
        Tasks task = buildTask(1L);
        User user = buildUser(1L);
        TaskAssignment assignment = buildAssignment(10L, task, user);
        when(taskAssignmentService.findByTaskId(1L)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/projects/1/tasks/1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getAssignmentsByTask_emptyList_returns200() throws Exception {
        when(taskAssignmentService.findByTaskId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/1/tasks/1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void assignUser_validPayload_returns201() throws Exception {
        Tasks task = buildTask(1L);
        User user = buildUser(2L);
        TaskAssignment saved = buildAssignment(20L, task, user);

        when(tasksService.findById(1L)).thenReturn(Optional.of(task));
        when(userService.findById(2L)).thenReturn(Optional.of(user));
        when(taskAssignmentService.save(any(TaskAssignment.class))).thenReturn(saved);

        String payload = """
                {"userId": 2}
                """;

        mockMvc.perform(post("/api/projects/1/tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void assignUser_missingUserId_returns400() throws Exception {
        String payload = """
                {}
                """;

        mockMvc.perform(post("/api/projects/1/tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("userId is required"));
    }

    @Test
    void assignUser_taskNotFound_returns404() throws Exception {
        when(tasksService.findById(999L)).thenReturn(Optional.empty());
        when(userService.findById(1L)).thenReturn(Optional.of(buildUser(1L)));

        String payload = """
                {"userId": 1}
                """;

        mockMvc.perform(post("/api/projects/1/tasks/999/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task or User not found"));
    }

    @Test
    void assignUser_userNotFound_returns404() throws Exception {
        when(tasksService.findById(1L)).thenReturn(Optional.of(buildTask(1L)));
        when(userService.findById(999L)).thenReturn(Optional.empty());

        String payload = """
                {"userId": 999}
                """;

        mockMvc.perform(post("/api/projects/1/tasks/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task or User not found"));
    }

    @Test
    void removeAssignment_found_returns204() throws Exception {
        Tasks task = buildTask(1L);
        User user = buildUser(2L);
        TaskAssignment assignment = buildAssignment(10L, task, user);

        when(taskAssignmentService.findByTaskIdAndUserId(1L, 2L)).thenReturn(Optional.of(assignment));
        doNothing().when(taskAssignmentService).deleteById(10L);

        mockMvc.perform(delete("/api/projects/1/tasks/1/assignments/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeAssignment_notFound_returns404() throws Exception {
        when(taskAssignmentService.findByTaskIdAndUserId(1L, 999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/projects/1/tasks/1/assignments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Assignment not found for this user and task"));
    }
}
