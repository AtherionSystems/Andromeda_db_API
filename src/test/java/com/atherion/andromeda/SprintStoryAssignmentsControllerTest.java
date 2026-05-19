package com.atherion.andromeda;

import com.atherion.andromeda.controllers.SprintStoryAssignments;
import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.services.SprintService;
import com.atherion.andromeda.services.SprintStoryAssignmentService;
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
class SprintStoryAssignmentsControllerTest {

    @Mock private SprintStoryAssignmentService sprintStoryAssignmentService;
    @Mock private SprintService sprintService;
    @Mock private UserStoryService userStoryService;
    @InjectMocks private SprintStoryAssignments controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Project buildProject(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setName("Project");
        project.setStatus("active");
        return project;
    }

    private Sprint buildSprint(Long id, Project project) {
        Sprint sprint = new Sprint();
        sprint.setId(id);
        sprint.setProject(project);
        sprint.setName("Sprint");
        sprint.setStatus("active");
        return sprint;
    }

    private UserStory buildUserStory(Long id, Project project) {
        UserStory story = new UserStory();
        story.setId(id);
        story.setTitle("Story");
        story.setStatus("todo");
        story.setPriority("medium");

        Capability capability = new Capability();
        capability.setId(20L);
        capability.setProject(project);

        Feature feature = new Feature();
        feature.setId(30L);
        feature.setCapability(capability);

        story.setFeature(feature);
        return story;
    }

    private SprintStoryAssignment buildAssignment(Long id, Sprint sprint, UserStory story) {
        SprintStoryAssignment assignment = new SprintStoryAssignment();
        assignment.setId(id);
        assignment.setSprint(sprint);
        assignment.setUserStoryId(story.getId());
        assignment.setIsActive(1);
        return assignment;
    }

    @Test
    void getSprintStoryAssignments_returns200() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        UserStory story = buildUserStory(3L, project);
        SprintStoryAssignment assignment = buildAssignment(4L, sprint, story);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintStoryAssignmentService.findBySprintId(2L)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/projects/1/sprints/2/user_stories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void createSprintStoryAssignment_missingUserStoryId_returns400() throws Exception {
        mockMvc.perform(post("/api/projects/1/sprints/2/user_stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("userStoryId is required"));
    }

    @Test
    void createSprintStoryAssignment_validPayload_returns201() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        UserStory story = buildUserStory(3L, project);
        SprintStoryAssignment saved = buildAssignment(4L, sprint, story);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(userStoryService.findById(3L)).thenReturn(Optional.of(story));
        when(sprintStoryAssignmentService.isStoryActiveInSprint(2L, 3L)).thenReturn(false);
        when(sprintStoryAssignmentService.save(any(SprintStoryAssignment.class))).thenReturn(saved);

        mockMvc.perform(post("/api/projects/1/sprints/2/user_stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userStoryId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void createSprintStoryAssignment_duplicate_returns409() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        UserStory story = buildUserStory(3L, project);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(userStoryService.findById(3L)).thenReturn(Optional.of(story));
        when(sprintStoryAssignmentService.isStoryActiveInSprint(2L, 3L)).thenReturn(true);

        mockMvc.perform(post("/api/projects/1/sprints/2/user_stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userStoryId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("User story is already active in this sprint"));
    }

    @Test
    void updateSprintStoryAssignment_notFound_returns404() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintStoryAssignmentService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/projects/1/sprints/2/user_stories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"removedAt\":\"2026-01-01T10:00:00\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Sprint story assignment not found"));
    }

    @Test
    void deleteSprintStoryAssignment_found_returns204() throws Exception {
        Project project = buildProject(1L);
        Sprint sprint = buildSprint(2L, project);
        UserStory story = buildUserStory(3L, project);
        SprintStoryAssignment assignment = buildAssignment(4L, sprint, story);

        when(sprintService.findById(2L)).thenReturn(Optional.of(sprint));
        when(sprintStoryAssignmentService.findById(4L)).thenReturn(Optional.of(assignment));
        doNothing().when(sprintStoryAssignmentService).deleteById(4L);

        mockMvc.perform(delete("/api/projects/1/sprints/2/user_stories/4"))
                .andExpect(status().isNoContent());
    }
}
