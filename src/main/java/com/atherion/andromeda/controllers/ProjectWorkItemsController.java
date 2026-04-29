package com.atherion.andromeda.controllers;

import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/work-items")
@RequiredArgsConstructor
public class ProjectWorkItemsController {

    private final ProjectService projectService;
    private final CapabilityService capabilityService;
    private final FeatureService featureService;
    private final UserStoryService userStoryService;
    private final TasksService tasksService;
    private final SprintService sprintService;
    private final SprintStoryAssignmentService sprintStoryAssignmentService;

    @GetMapping
    public ResponseEntity<?> getProjectWorkItems(@PathVariable Long projectId) {
        Project project = projectService.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Project not found"));
        }

        List<Capability> capabilities = capabilityService.findByProjectId(projectId);
        List<Feature> features = featureService.findByProjectId(projectId);
        List<UserStory> stories = userStoryService.findByProjectId(projectId);
        List<Tasks> tasks = tasksService.findByProjectId(projectId);
        List<Sprint> sprints = sprintService.findByProjectId(projectId);

        Map<Long, List<Feature>> featuresByCapability = features.stream()
                .collect(Collectors.groupingBy(f -> f.getCapability().getId()));
        Map<Long, List<UserStory>> storiesByFeature = stories.stream()
                .collect(Collectors.groupingBy(s -> s.getFeature().getId()));
        Map<Long, List<Tasks>> tasksByStory = tasks.stream()
                .filter(t -> t.getUserStoryId() != null)
                .collect(Collectors.groupingBy(Tasks::getUserStoryId));

        Map<Long, List<Long>> sprintIdsByStory = new HashMap<>();
        List<SprintNode> sprintNodes = new ArrayList<>();
        for (Sprint sprint : sprints) {
            List<SprintStoryAssignment> assignments = sprintStoryAssignmentService.findBySprintId(sprint.getId()).stream()
                    .filter(a -> a.getIsActive() != null && a.getIsActive() == 1 && a.getRemovedAt() == null)
                    .toList();
            List<Long> storyIds = assignments.stream().map(SprintStoryAssignment::getUserStoryId).toList();
            for (Long storyId : storyIds) {
                sprintIdsByStory.computeIfAbsent(storyId, ignored -> new ArrayList<>()).add(sprint.getId());
            }
            sprintNodes.add(new SprintNode(
                    sprint.getId(),
                    sprint.getName(),
                    sprint.getStatus(),
                    sprint.getStartDate(),
                    sprint.getDueDate(),
                    storyIds
            ));
        }

        List<CapabilityNode> capabilityNodes = new ArrayList<>();
        for (Capability capability : capabilities) {
            List<FeatureNode> featureNodes = new ArrayList<>();
            for (Feature feature : featuresByCapability.getOrDefault(capability.getId(), List.of())) {
                List<UserStoryNode> storyNodes = new ArrayList<>();
                for (UserStory story : storiesByFeature.getOrDefault(feature.getId(), List.of())) {
                    List<TaskNode> taskNodes = tasksByStory.getOrDefault(story.getId(), List.of()).stream()
                            .map(t -> new TaskNode(
                                    t.getId(),
                                    t.getTitle(),
                                    t.getStatus(),
                                    t.getPriority(),
                                    t.getEstimatedHours(),
                                    t.getActualHours(),
                                    sprintIdsByStory.getOrDefault(story.getId(), List.of())
                            ))
                            .toList();
                    storyNodes.add(new UserStoryNode(
                            story.getId(),
                            story.getTitle(),
                            story.getStatus(),
                            story.getPriority(),
                            story.getStoryPoints(),
                            taskNodes
                    ));
                }
                featureNodes.add(new FeatureNode(feature.getId(), feature.getName(), feature.getStatus(), storyNodes));
            }
            capabilityNodes.add(new CapabilityNode(capability.getId(), capability.getName(), capability.getStatus(), featureNodes));
        }

        return ResponseEntity.ok(new ProjectWorkItemsResponse(
                new ProjectSummary(project.getId(), project.getName(), project.getStatus()),
                capabilityNodes,
                sprintNodes
        ));
    }

    private record ProjectWorkItemsResponse(ProjectSummary project,
                                            List<CapabilityNode> capabilities,
                                            List<SprintNode> sprints) {}

    private record ProjectSummary(Long id, String name, String status) {}

    private record CapabilityNode(Long id, String name, String status, List<FeatureNode> features) {}

    private record FeatureNode(Long id, String name, String status, List<UserStoryNode> stories) {}

    private record UserStoryNode(Long id, String title, String status, String priority, Integer storyPoints, List<TaskNode> tasks) {}

    private record TaskNode(Long id, String title, String status, String priority, java.math.BigDecimal estimatedHours,
                            java.math.BigDecimal actualHours, List<Long> sprintIds) {}

    private record SprintNode(Long id, String name, String status, LocalDateTime startDate, LocalDateTime dueDate,
                              List<Long> activeStoryIds) {}
}
