package com.atherion.andromeda;

import com.atherion.andromeda.controllers.FeaturesController;
import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.services.CapabilityService;
import com.atherion.andromeda.services.FeatureService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FeaturesControllerTest {
    @Mock private FeatureService featureService;
    @Mock private CapabilityService capabilityService;
    @InjectMocks private FeaturesController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByCapability_capabilityNotFound_returns404() throws Exception {
        when(capabilityService.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/capabilities/2/features"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Capability not found"));
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        Project project = new Project();
        project.setId(1L);
        Capability capability = new Capability();
        capability.setId(2L);
        capability.setProject(project);

        Feature saved = new Feature();
        saved.setId(3L);
        saved.setName("Feature A");
        saved.setCapability(capability);
        saved.setStatus("active");

        when(capabilityService.findById(2L)).thenReturn(Optional.of(capability));
        when(featureService.save(any(Feature.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects/1/capabilities/2/features")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Feature A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Feature A"));
    }
}
