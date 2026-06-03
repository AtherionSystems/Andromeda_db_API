package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.AssignedUserSummary;
import com.atherion.andromeda.dto.SprintStoryAssignmentResponse;
import com.atherion.andromeda.dto.SprintTaskAssigneeRow;
import com.atherion.andromeda.dto.SprintTaskRow;
import com.atherion.andromeda.dto.SprintTaskSummary;
import com.atherion.andromeda.dto.UserStorySummary;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.TaskAssignment;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.repositories.SprintRepository;
import com.atherion.andromeda.repositories.SprintStoryAssignmentRepository;
import com.atherion.andromeda.repositories.TaskAssignmentRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintStoryAssignmentService {
    private final SprintStoryAssignmentRepository sprintStoryAssignmentRepository;
    private final SprintRepository sprintRepository;
    private final TasksRepository tasksRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final UserStoryRepository userStoryRepository;

    public List<SprintStoryAssignment> findAll() { return sprintStoryAssignmentRepository.findAll(); }
    public List<SprintStoryAssignment> findBySprintId(Long sprintId) { return sprintStoryAssignmentRepository.findBySprint_Id(sprintId); }
    public Optional<SprintStoryAssignment> findById(Long id) { return sprintStoryAssignmentRepository.findById(id); }
    public boolean isStoryActiveInSprint(Long sprintId, Long userStoryId) {
        return sprintStoryAssignmentRepository.existsBySprint_IdAndUserStoryIdAndIsActiveAndRemovedAtIsNull(
                sprintId,
                userStoryId,
                1
        );
    }

    public List<SprintTaskRow> findSprintBoard(Long projectId) {
        List<Sprint> latestSprints = sprintRepository.findTop2ByProject_IdOrderByCreatedAtDesc(projectId);
        if (latestSprints.isEmpty()) {
            return List.of();
        }

        List<Long> sprintIds = latestSprints.stream().map(Sprint::getId).toList();
        Map<Long, String> sprintNames = latestSprints.stream()
                .collect(Collectors.toMap(Sprint::getId, Sprint::getName));

        List<SprintStoryAssignment> activeAssignments =
                sprintStoryAssignmentRepository.findBySprint_IdInAndIsActiveAndRemovedAtIsNull(sprintIds, 1);
        if (activeAssignments.isEmpty()) {
            return List.of();
        }

        List<Long> storyIds = activeAssignments.stream()
                .map(SprintStoryAssignment::getUserStoryId)
                .distinct()
                .toList();
        List<Tasks> tasks = tasksRepository.findByUserStoryIdIn(storyIds).stream()
                .filter(t -> t.getProject() != null && t.getProject().getId().equals(projectId))
                .toList();
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Tasks>> tasksByStory = tasks.stream()
                .collect(Collectors.groupingBy(Tasks::getUserStoryId));
        List<Long> taskIds = tasks.stream().map(Tasks::getId).toList();
        Map<Long, String> assigneesByTaskId = buildAssigneesByTask(taskIds);

        List<SprintTaskRowValue> rows = new ArrayList<>();
        for (SprintStoryAssignment assignment : activeAssignments) {
            List<Tasks> storyTasks = tasksByStory.getOrDefault(assignment.getUserStoryId(), List.of());
            for (Tasks task : storyTasks) {
                rows.add(new SprintTaskRowValue(
                        task.getId(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority(),
                        task.getEstimatedHours(),
                        task.getActualHours(),
                        assigneesByTaskId.getOrDefault(task.getId(), ""),
                        sprintNames.getOrDefault(assignment.getSprint().getId(), assignment.getSprint().getName()),
                        assignment.getSprint().getId()
                ));
            }
        }

        rows.sort(Comparator
                .comparing(SprintTaskRowValue::getSprintId).reversed()
                .thenComparingInt(row -> statusOrder(row.getStatus()))
                .thenComparingInt(row -> priorityOrder(row.getPriority())));
        return new ArrayList<>(rows);
    }

    public List<SprintStoryAssignmentResponse> findBySprintIdAsResponse(Long sprintId) {
        return enrichWithDetails(sprintStoryAssignmentRepository.findBySprintIdAsResponse(sprintId));
    }

    public Optional<SprintStoryAssignmentResponse> findByIdAsResponse(Long id) {
        return sprintStoryAssignmentRepository.findByIdAsResponse(id)
                .map(ssa -> enrichWithDetails(List.of(ssa)).get(0));
    }

    private List<SprintStoryAssignmentResponse> enrichWithDetails(List<SprintStoryAssignmentResponse> assignments) {
        if (assignments.isEmpty()) return assignments;

        List<Long> storyIds = assignments.stream()
                .map(SprintStoryAssignmentResponse::userStoryId)
                .distinct()
                .toList();

        // Query 2: tasks con assignees agrupadas por storyId → taskId
        Map<Long, Map<Long, SprintTaskSummaryBuilder>> buildersByStory = new HashMap<>();
        for (SprintTaskAssigneeRow row : tasksRepository.findTasksWithAssigneesByStoryIds(storyIds)) {
            buildersByStory
                    .computeIfAbsent(row.userStoryId(), ignored -> new HashMap<>())
                    .computeIfAbsent(row.taskId(), ignored -> new SprintTaskSummaryBuilder(row))
                    .addAssignee(row.assigneeUserId(), row.assigneeUserName());
        }
        Map<Long, List<SprintTaskSummary>> tasksByStory = new HashMap<>();
        buildersByStory.forEach((storyId, builders) ->
                tasksByStory.put(storyId, builders.values().stream().map(SprintTaskSummaryBuilder::build).toList()));

        // Query 3: user story summaries indexadas por id
        Map<Long, UserStorySummary> storiesById = userStoryRepository.findSummariesByIds(storyIds)
                .stream()
                .collect(Collectors.toMap(UserStorySummary::id, s -> s));

        return assignments.stream()
                .map(a -> a.withDetails(
                        storiesById.get(a.userStoryId()),
                        tasksByStory.getOrDefault(a.userStoryId(), List.of())
                ))
                .toList();
    }

    private static final class SprintTaskSummaryBuilder {
        private final SprintTaskAssigneeRow base;
        private final List<AssignedUserSummary> assignees = new ArrayList<>();

        SprintTaskSummaryBuilder(SprintTaskAssigneeRow base) {
            this.base = base;
        }

        void addAssignee(Long userId, String userName) {
            if (userId != null) assignees.add(new AssignedUserSummary(userId, userName));
        }

        SprintTaskSummary build() {
            return new SprintTaskSummary(base.taskId(), base.taskTitle(), base.taskPriority(),
                    base.taskStatus(), base.taskDueDate(), base.estimatedHours(), base.actualHours(),
                    List.copyOf(assignees));
        }
    }

    public SprintStoryAssignment save(SprintStoryAssignment sprintStoryAssignment) { return sprintStoryAssignmentRepository.save(sprintStoryAssignment); }
    public void deleteById(Long id) { sprintStoryAssignmentRepository.deleteById(id); }

    private Map<Long, String> buildAssigneesByTask(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTask_IdIn(taskIds);
        Map<Long, List<String>> usernamesByTask = new HashMap<>();
        for (TaskAssignment assignment : assignments) {
            if (assignment.getTask() == null || assignment.getUser() == null || assignment.getUser().getUsername() == null) {
                continue;
            }
            usernamesByTask
                    .computeIfAbsent(assignment.getTask().getId(), ignored -> new ArrayList<>())
                    .add(assignment.getUser().getUsername());
        }

        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : usernamesByTask.entrySet()) {
            List<String> sorted = entry.getValue().stream().distinct().sorted().toList();
            result.put(entry.getKey(), String.join(", ", sorted));
        }
        return result;
    }

    private int statusOrder(String status) {
        if ("in_progress".equals(status)) return 1;
        if ("review".equals(status)) return 2;
        if ("todo".equals(status)) return 3;
        if ("done".equals(status)) return 4;
        return 5;
    }

    private int priorityOrder(String priority) {
        if ("critical".equals(priority)) return 1;
        if ("high".equals(priority)) return 2;
        if ("medium".equals(priority)) return 3;
        if ("low".equals(priority)) return 4;
        return 5;
    }

    private static final class SprintTaskRowValue implements SprintTaskRow {
        private final Long id;
        private final String title;
        private final String status;
        private final String priority;
        private final BigDecimal estimatedHours;
        private final BigDecimal actualHours;
        private final String assignees;
        private final String sprintName;
        private final Long sprintId;

        private SprintTaskRowValue(
                Long id,
                String title,
                String status,
                String priority,
                BigDecimal estimatedHours,
                BigDecimal actualHours,
                String assignees,
                String sprintName,
                Long sprintId
        ) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.priority = priority;
            this.estimatedHours = estimatedHours;
            this.actualHours = actualHours;
            this.assignees = assignees;
            this.sprintName = sprintName;
            this.sprintId = sprintId;
        }

        @Override
        public Long getId() { return id; }

        @Override
        public String getTitle() { return title; }

        @Override
        public String getStatus() { return status; }

        @Override
        public String getPriority() { return priority; }

        @Override
        public BigDecimal getEstimatedHours() { return estimatedHours; }

        @Override
        public BigDecimal getActualHours() { return actualHours; }

        @Override
        public String getAssignees() { return assignees; }

        @Override
        public String getSprintName() { return sprintName; }

        public Long getSprintId() { return sprintId; }
    }
}
