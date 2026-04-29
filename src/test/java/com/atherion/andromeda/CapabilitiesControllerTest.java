package com.atherion.andromeda;

import com.atherion.andromeda.controllers.CapabilitiesController;
import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.CapabilityService;
import com.atherion.andromeda.services.ProjectService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CapabilitiesControllerTest {
    @Mock private CapabilityService capabilityService;
    @Mock private ProjectService projectService;
    @InjectMocks private CapabilitiesController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByProject_returns200() throws Exception {
        Project project = new Project();
        project.setId(1L);
        Capability capability = new Capability();
        capability.setId(2L);
        capability.setName("Auth");
        capability.setProject(project);

        when(projectService.findById(1L)).thenReturn(Optional.of(project));
        when(capabilityService.findByProjectId(1L)).thenReturn(List.of(capability));

        mockMvc.perform(get("/api/projects/1/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Auth"));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/projects/1/capabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("name is required"));
    }
}
