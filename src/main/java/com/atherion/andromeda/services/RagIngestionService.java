package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.StoryAssigneeRow;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.repositories.SprintRepository;
import com.atherion.andromeda.repositories.TaskAssignmentRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagIngestionService {

    private final UserStoryRepository userStoryRepository;
    private final TasksRepository tasksRepository;
    private final SprintRepository sprintRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Transactional(readOnly = true)
    public int ingest(Long projectId) {
        int count = 0;

        List<UserStory> stories = projectId != null
                ? userStoryRepository.findByFeature_Capability_Project_Id(projectId)
                : userStoryRepository.findAll();

        if (!stories.isEmpty()) {
            List<Long> storyIds = stories.stream().map(UserStory::getId).toList();
            Map<Long, List<String>> assigneesByStory = userStoryRepository
                    .findAssigneesByStoryIds(storyIds).stream()
                    .collect(Collectors.groupingBy(StoryAssigneeRow::storyId,
                            Collectors.mapping(StoryAssigneeRow::userName, Collectors.toList())));

            for (UserStory s : stories) {
                Long pid = projectId != null ? projectId : resolveStoryProjectId(s);
                String ownerName = s.getOwner() != null ? s.getOwner().getName() : null;
                List<String> assignees = assigneesByStory.getOrDefault(s.getId(), List.of());
                String text = formatUserStory(s, ownerName, assignees);
                float[] vector = embeddingService.embed(text);
                if (vector != null) {
                    vectorStoreService.upsert("user_story", s.getId(), pid, vector, text);
                    count++;
                }
            }
        }

        List<Tasks> tasks = projectId != null
                ? tasksRepository.findByProject_Id(projectId)
                : tasksRepository.findAll();

        if (!tasks.isEmpty()) {
            List<Long> taskIds = tasks.stream().map(Tasks::getId).toList();
            Map<Long, List<String>> assigneesByTask = taskAssignmentRepository
                    .findByTaskIdsWithDetails(taskIds).stream()
                    .collect(Collectors.groupingBy(ta -> ta.getTask().getId(),
                            Collectors.mapping(ta -> ta.getUser().getName(), Collectors.toList())));

            for (Tasks t : tasks) {
                Long pid = t.getProject() != null ? t.getProject().getId() : null;
                List<String> assignees = assigneesByTask.getOrDefault(t.getId(), List.of());
                String text = formatTask(t, assignees);
                float[] vector = embeddingService.embed(text);
                if (vector != null) {
                    vectorStoreService.upsert("task", t.getId(), pid, vector, text);
                    count++;
                }
            }
        }

        List<Sprint> sprints = projectId != null
                ? sprintRepository.findByProject_Id(projectId)
                : sprintRepository.findAll();

        for (Sprint s : sprints) {
            Long pid = s.getProject() != null ? s.getProject().getId() : null;
            String text = formatSprint(s);
            float[] vector = embeddingService.embed(text);
            if (vector != null) {
                vectorStoreService.upsert("sprint", s.getId(), pid, vector, text);
                count++;
            }
        }

        log.info("RAG ingestion complete: {} documents indexed (projectId={})", count, projectId);
        return count;
    }

    @Async
    @Transactional(readOnly = true)
    public void ingestUserStoryAsync(Long storyId) {
        userStoryRepository.findById(storyId).ifPresent(story -> {
            Long pid = resolveStoryProjectId(story);
            String ownerName = story.getOwner() != null ? story.getOwner().getName() : null;
            List<String> assignees = userStoryRepository.findAssigneesByStoryIds(List.of(storyId))
                    .stream().map(StoryAssigneeRow::userName).toList();
            String text = formatUserStory(story, ownerName, assignees);
            float[] vector = embeddingService.embed(text);
            if (vector != null) vectorStoreService.upsert("user_story", storyId, pid, vector, text);
        });
    }

    @Async
    @Transactional(readOnly = true)
    public void ingestTaskAsync(Long taskId) {
        tasksRepository.findById(taskId).ifPresent(task -> {
            Long pid = task.getProject() != null ? task.getProject().getId() : null;
            List<String> assignees = taskAssignmentRepository.findByTaskIdWithDetails(taskId)
                    .stream().map(ta -> ta.getUser().getName()).toList();
            String text = formatTask(task, assignees);
            float[] vector = embeddingService.embed(text);
            if (vector != null) vectorStoreService.upsert("task", taskId, pid, vector, text);
        });
    }

    @Async
    @Transactional(readOnly = true)
    public void ingestSprintAsync(Long sprintId) {
        sprintRepository.findById(sprintId).ifPresent(sprint -> {
            Long pid = sprint.getProject() != null ? sprint.getProject().getId() : null;
            String text = formatSprint(sprint);
            float[] vector = embeddingService.embed(text);
            if (vector != null) vectorStoreService.upsert("sprint", sprintId, pid, vector, text);
        });
    }

    @Async
    public void deleteAsync(String type, Long entityId) {
        vectorStoreService.delete(type, entityId);
    }

    private Long resolveStoryProjectId(UserStory s) {
        try {
            return s.getFeature().getCapability().getProject().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatUserStory(UserStory s, String ownerName, List<String> assignees) {
        return "[UserStory #" + s.getId() + "] " + s.getTitle() +
                "\nEstado: " + nvl(s.getStatus()) +
                " | Prioridad: " + nvl(s.getPriority()) +
                " | Puntos: " + nvl(s.getStoryPoints()) +
                "\nOwner: " + nvl(ownerName) +
                (assignees.isEmpty() ? "" : "\nAsignados: " + String.join(", ", assignees)) +
                "\nDescripción: " + nvl(s.getDescription()) +
                "\nCriterios de aceptación: " + nvl(s.getAcceptanceCriteria());
    }

    private String formatTask(Tasks t, List<String> assignees) {
        return "[Task #" + t.getId() + "] " + t.getTitle() +
                "\nEstado: " + nvl(t.getStatus()) +
                " | Prioridad: " + nvl(t.getPriority()) +
                " | Horas estimadas: " + nvl(t.getEstimatedHours()) +
                (assignees.isEmpty() ? "" : "\nAsignados: " + String.join(", ", assignees)) +
                "\nDescripción: " + nvl(t.getDescription());
    }

    private String formatSprint(Sprint s) {
        return "[Sprint #" + s.getId() + "] " + s.getName() +
                "\nEstado: " + nvl(s.getStatus()) +
                " | Inicio: " + nvl(s.getStartDate()) +
                " | Fin: " + nvl(s.getDueDate()) +
                "\nObjetivo: " + nvl(s.getGoal());
    }

    private String nvl(Object val) {
        return val == null ? "—" : val.toString();
    }
}
