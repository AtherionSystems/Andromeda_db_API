package com.atherion.andromeda;

import com.atherion.andromeda.controllers.UserStoriesController;
import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.FeatureService;
import com.atherion.andromeda.services.UserService;
import com.atherion.andromeda.services.UserStoryService;
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
class UserStoriesControllerTest {
    @Mock private UserStoryService userStoryService;
    @Mock private FeatureService featureService;
    @Mock private UserService userService;
    @InjectMocks private UserStoriesController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(userStoryService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/capabilities/2/features/3/stories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User story not found"));
    }

    @Test
    void create_missingCreatedById_returns400() throws Exception {
        Project project = new Project();
        project.setId(1L);
        Capability capability = new Capability();
        capability.setId(2L);
        capability.setProject(project);
        Feature feature = new Feature();
        feature.setId(3L);
        feature.setCapability(capability);

        when(featureService.findById(3L)).thenReturn(Optional.of(feature));

        mockMvc.perform(post("/api/projects/1/capabilities/2/features/3/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"US-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("createdById is required"));
    }
}
