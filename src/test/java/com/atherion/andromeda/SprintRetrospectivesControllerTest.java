package com.atherion.andromeda;

import com.atherion.andromeda.controllers.SprintRetrospectivesController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.services.SprintRetrospectiveService;
import com.atherion.andromeda.services.SprintService;
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

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SprintRetrospectivesControllerTest {
    @Mock private SprintRetrospectiveService sprintRetrospectiveService;
    @Mock private SprintService sprintService;
    @Mock private UserService userService;
    @InjectMocks private SprintRetrospectivesController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getBySprint_sprintNotFound_returns404() throws Exception {
        when(sprintService.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/sprints/2/retrospective"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sprint not found"));
    }

    @Test
    void create_missingCreatedById_returns400() throws Exception {
        Project project = new Project();
        project.setId(1L);
        Sprint sprint = new Sprint();
        sprint.setId(2L);
        sprint.setProject(project);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintRetrospectiveService.findBySprintId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/projects/1/sprints/2/retrospective")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"summary\":\"ok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("createdById is required"));
    }
}
