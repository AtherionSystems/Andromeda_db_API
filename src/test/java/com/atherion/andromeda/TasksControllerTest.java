package com.atherion.andromeda;

import com.atherion.andromeda.controllers.TasksController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.TasksService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TasksControllerTest {

    @Mock private TasksService tasksService;
    @Mock private ProjectService projectService;

    @InjectMocks private TasksController controller;

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Project buildProject(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setName("Test Project");
        p.setStatus("active");
        return p;
    }

    private Tasks buildTask(Long id, Project project) {
        Tasks t = new Tasks();
        t.setId(id);
        t.setTitle("Test Task");
        t.setPriority("medium");
        t.setStatus("todo");
        t.setProject(project);
        return t;
    }

    @Test
    void getTasksByProject_returns200WithList() throws Exception {
        Project project = buildProject(1L);
        Tasks task = buildTask(1L, project);
        when(tasksService.findByProjectId(1L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    @Test
    void getTaskById_found_returns200() throws Exception {
        Project project = buildProject(1L);
        Tasks task = buildTask(1L, project);
        when(tasksService.findById(1L)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/projects/1/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.status").value("todo"));
    }

    @Test
    void getTaskById_notFound_returns404() throws Exception {
        when(tasksService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }

    @Test
    void createTask_validPayload_returns201() throws Exception {
        Project project = buildProject(1L);
        Tasks saved = buildTask(10L, project);
        when(projectService.findById(1L)).thenReturn(Optional.of(project));
        when(tasksService.save(any(Tasks.class))).thenReturn(saved);

        String payload = """
                {"title": "New Task", "priority": "high"}
                """;

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void createTask_projectNotFound_returns404() throws Exception {
        when(projectService.findById(999L)).thenReturn(Optional.empty());

        String payload = """
                {"title": "Orphan Task"}
                """;

        mockMvc.perform(post("/api/projects/999/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void createTask_missingTitle_returns400() throws Exception {
        String payload = """
                {"description": "No title provided"}
                """;

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("title is required"));
    }

    @Test
    void updateTask_found_returns200() throws Exception {
        Project project = buildProject(1L);
        Tasks existing = buildTask(1L, project);
        Tasks updated = buildTask(1L, project);
        updated.setTitle("Updated Title");
        updated.setStatus("in_progress");

        when(tasksService.findById(1L)).thenReturn(Optional.of(existing));
        when(tasksService.save(any(Tasks.class))).thenReturn(updated);

        String payload = """
                {"title": "Updated Title", "status": "in_progress"}
                """;

        mockMvc.perform(patch("/api/projects/1/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("in_progress"));
    }

    @Test
    void updateTask_notFound_returns404() throws Exception {
        when(tasksService.findById(999L)).thenReturn(Optional.empty());

        String payload = """
                {"title": "Does not matter"}
                """;

        mockMvc.perform(patch("/api/projects/1/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }

    @Test
    void deleteTask_found_returns204() throws Exception {
        Project project = buildProject(1L);
        Tasks task = buildTask(1L, project);
        when(tasksService.findById(1L)).thenReturn(Optional.of(task));
        doNothing().when(tasksService).deleteById(1L);

        mockMvc.perform(delete("/api/projects/1/tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_notFound_returns404() throws Exception {
        when(tasksService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/projects/1/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }
}
