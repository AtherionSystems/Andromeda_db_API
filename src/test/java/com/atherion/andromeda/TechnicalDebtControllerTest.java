package com.atherion.andromeda;

import com.atherion.andromeda.controllers.TechnicalDebtController;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.*;
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
class TechnicalDebtControllerTest {
    @Mock private TechnicalDebtService technicalDebtService;
    @Mock private UserService userService;
    @Mock private UserStoryService userStoryService;
    @Mock private TasksService tasksService;
    @Mock private ProjectService projectService;
    @InjectMocks private TechnicalDebtController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByProject_projectNotFound_returns404() throws Exception {
        when(projectService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/technical-debt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        Project project = new Project();
        project.setId(1L);
        when(projectService.findById(1L)).thenReturn(Optional.of(project));

        mockMvc.perform(post("/api/projects/1/technical-debt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Debt A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("title, debtType, assignedToId and createdById are required"));
    }
}
