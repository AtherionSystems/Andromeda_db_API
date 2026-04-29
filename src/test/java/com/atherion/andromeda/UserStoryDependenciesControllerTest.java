package com.atherion.andromeda;

import com.atherion.andromeda.controllers.UserStoryDependenciesController;
import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.UserStoryDependencyService;
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
class UserStoryDependenciesControllerTest {
    @Mock private UserStoryService userStoryService;
    @Mock private UserStoryDependencyService dependencyService;
    @InjectMocks private UserStoryDependenciesController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getByStory_storyNotFound_returns404() throws Exception {
        when(userStoryService.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/stories/10/dependencies"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User story not found"));
    }

    @Test
    void create_missingBlockedById_returns400() throws Exception {
        Project project = new Project();
        project.setId(1L);
        Capability capability = new Capability();
        capability.setProject(project);
        Feature feature = new Feature();
        feature.setCapability(capability);
        UserStory story = new UserStory();
        story.setId(10L);
        story.setFeature(feature);

        when(userStoryService.findById(10L)).thenReturn(Optional.of(story));

        mockMvc.perform(post("/api/projects/1/stories/10/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("blockedById is required"));
    }
}
