package com.atherion.andromeda;

import com.atherion.andromeda.controllers.SprintTasksController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintTask;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.SprintTaskService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SprintTasksControllerTest {

    @Mock private SprintTaskService sprintTaskService;
    @Mock private SprintService sprintService;
    @Mock private TasksService tasksService;
    @InjectMocks private SprintTasksController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Project buildProject(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setName("Project");
        p.setStatus("active");
        return p;
    }

    private Sprint buildSprint(Long id, Project project) {
        Sprint s = new Sprint();
        s.setId(id);
        s.setProject(project);
        s.setName("Sprint");
        s.setStatus("active");
        return s;
    }

    private Tasks buildTask(Long id, Project project) {
        Tasks t = new Tasks();
        t.setId(id);
        t.setProject(project);
        t.setTitle("Task");
        t.setStatus("todo");
        return t;
    }

    private SprintTask buildSprintTask(Long id, Sprint sprint, Tasks task) {
        SprintTask st = new SprintTask();
        st.setId(id);
        st.setSprint(sprint);
        st.setTask(task);
        return st;
    }

    @Test
    void getSprintTasks_returns200() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        Tasks task = buildTask(3L, project);
        SprintTask sprintTask = buildSprintTask(4L, sprint, task);
        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintTaskService.findBySprintId(2L)).thenReturn(List.of(sprintTask));

        mockMvc.perform(get("/api/projects/1/sprints/2/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void createSprintTask_missingTaskId_returns400() throws Exception {
        mockMvc.perform(post("/api/projects/1/sprints/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("taskId is required"));
    }

    @Test
    void createSprintTask_validPayload_returns201() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        Tasks task = buildTask(3L, project);
        SprintTask saved = buildSprintTask(5L, sprint, task);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(tasksService.findById(3L)).thenReturn(Optional.of(task));
        when(sprintTaskService.isTaskActiveInSprint(2L, 3L)).thenReturn(false);
        when(sprintTaskService.save(any(SprintTask.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects/1/sprints/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createSprintTask_duplicate_returns409() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        Tasks task = buildTask(3L, project);
        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(tasksService.findById(3L)).thenReturn(Optional.of(task));
        when(sprintTaskService.isTaskActiveInSprint(2L, 3L)).thenReturn(true);

        mockMvc.perform(post("/api/projects/1/sprints/2/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Task is already active in this sprint"));
    }

    @Test
    void updateSprintTask_notFound_returns404() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintTaskService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/projects/1/sprints/2/tasks/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"removedAt\":\"2026-01-01T10:00:00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sprint task not found"));
    }

    @Test
    void deleteSprintTask_found_returns204() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        Tasks task = buildTask(3L, project);
        SprintTask sprintTask = buildSprintTask(4L, sprint, task);
        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintTaskService.findById(4L)).thenReturn(Optional.of(sprintTask));
        doNothing().when(sprintTaskService).deleteById(4L);

        mockMvc.perform(delete("/api/projects/1/sprints/2/tasks/4"))
                .andExpect(status().isNoContent());
    }
}
