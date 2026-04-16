package com.atherion.andromeda;

import com.atherion.andromeda.controllers.SprintsController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.SprintService;
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
class SprintsControllerTest {

    @Mock private SprintService sprintService;
    @Mock private ProjectService projectService;
    @InjectMocks private SprintsController controller;

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

    private Sprint buildSprint(Long sprintId, Project project) {
        Sprint s = new Sprint();
        s.setId(sprintId);
        s.setProject(project);
        s.setName("Sprint 1");
        s.setStatus("planned");
        return s;
    }

    @Test
    void getSprintsByProject_returns200() throws Exception {
        Project p = buildProject(1L);
        Sprint s = buildSprint(10L, p);
        when(projectService.findById(1L)).thenReturn(Optional.of(p));
        when(sprintService.findByProjectId(1L)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/projects/1/sprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Sprint 1"));
    }

    @Test
    void getSprintById_notFound_returns404() throws Exception {
        when(sprintService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/sprints/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sprint not found"));
    }

    @Test
    void createSprint_validPayload_returns201() throws Exception {
        Project p = buildProject(1L);
        Sprint saved = buildSprint(21L, p);
        when(projectService.findById(1L)).thenReturn(Optional.of(p));
        when(sprintService.save(any(Sprint.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Backend Sprint\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.status").value("planned"));
    }

    @Test
    void createSprint_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/projects/1/sprints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"No name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("name is required"));
    }

    @Test
    void updateSprint_found_returns200() throws Exception {
        Project p = buildProject(1L);
        Sprint existing = buildSprint(10L, p);
        Sprint updated = buildSprint(10L, p);
        updated.setName("Sprint Updated");
        when(sprintService.findById(10L)).thenReturn(Optional.of(existing));
        when(sprintService.save(any(Sprint.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/projects/1/sprints/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sprint Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sprint Updated"));
    }

    @Test
    void deleteSprint_found_returns204() throws Exception {
        Project p = buildProject(1L);
        Sprint existing = buildSprint(10L, p);
        when(sprintService.findById(10L)).thenReturn(Optional.of(existing));
        doNothing().when(sprintService).deleteById(10L);

        mockMvc.perform(delete("/api/projects/1/sprints/10"))
                .andExpect(status().isNoContent());
    }
}
