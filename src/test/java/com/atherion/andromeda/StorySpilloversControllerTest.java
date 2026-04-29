package com.atherion.andromeda;

import com.atherion.andromeda.controllers.StorySpilloversController;
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
class StorySpilloversControllerTest {
    @Mock private StorySpilloverService spilloverService;
    @Mock private SprintStoryAssignmentService sprintStoryAssignmentService;
    @Mock private UserStoryService userStoryService;
    @Mock private SprintService sprintService;
    @Mock private UserService userService;
    @InjectMocks private StorySpilloversController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(spilloverService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/story-spillovers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Story spillover not found"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/projects/1/story-spillovers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"blocked\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sprintStoryId, userStoryId, originSprintId, destinationSprintId, createdById and reason are required"));
    }
}
