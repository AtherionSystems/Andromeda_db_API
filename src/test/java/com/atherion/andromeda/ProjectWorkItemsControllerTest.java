package com.atherion.andromeda;

import com.atherion.andromeda.controllers.ProjectWorkItemsController;
import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectWorkItemsControllerTest {
    @Mock private ProjectService projectService;
    @Mock private CapabilityService capabilityService;
    @Mock private FeatureService featureService;
    @Mock private UserStoryService userStoryService;
    @Mock private TasksService tasksService;
    @Mock private SprintService sprintService;
    @Mock private SprintStoryAssignmentService sprintStoryAssignmentService;
    @InjectMocks private ProjectWorkItemsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getProjectWorkItems_projectNotFound_returns404() throws Exception {
        when(projectService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/projects/1/work-items"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Project not found"));
    }

    @Test
    void getProjectWorkItems_returnsHierarchy() throws Exception {
        Project project = new Project();
        project.setId(1L);
        project.setName("Andromeda");
        project.setStatus("active");

        Capability capability = new Capability();
        capability.setId(10L);
        capability.setName("Capability A");
        capability.setStatus("active");
        capability.setProject(project);

        Feature feature = new Feature();
        feature.setId(20L);
        feature.setName("Feature A");
        feature.setStatus("active");
        feature.setCapability(capability);

        UserStory story = new UserStory();
        story.setId(30L);
        story.setTitle("Story A");
        story.setStatus("todo");
        story.setPriority("high");
        story.setStoryPoints(3);
        story.setFeature(feature);

        Tasks task = new Tasks();
        task.setId(40L);
        task.setTitle("Task A");
        task.setStatus("todo");
        task.setPriority("medium");
        task.setProject(project);
        task.setUserStoryId(30L);

        Sprint sprint = new Sprint();
        sprint.setId(50L);
        sprint.setName("Sprint 1");
        sprint.setStatus("planned");
        sprint.setProject(project);

        SprintStoryAssignment assignment = new SprintStoryAssignment();
        assignment.setId(60L);
        assignment.setSprint(sprint);
        assignment.setUserStoryId(30L);
        assignment.setIsActive(1);

        when(projectService.findById(1L)).thenReturn(Optional.of(project));
        when(capabilityService.findByProjectId(1L)).thenReturn(List.of(capability));
        when(featureService.findByProjectId(1L)).thenReturn(List.of(feature));
        when(userStoryService.findByProjectId(1L)).thenReturn(List.of(story));
        when(tasksService.findByProjectId(1L)).thenReturn(List.of(task));
        when(sprintService.findByProjectId(1L)).thenReturn(List.of(sprint));
        when(sprintStoryAssignmentService.findBySprintId(50L)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/projects/1/work-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.id").value(1))
                .andExpect(jsonPath("$.capabilities[0].features[0].stories[0].tasks[0].id").value(40))
                .andExpect(jsonPath("$.sprints[0].activeStoryIds[0]").value(30));
    }
}
