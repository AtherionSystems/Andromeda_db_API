package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.repositories.SprintRepository;
import com.atherion.andromeda.repositories.TasksRepository;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagIngestionService {

    private final UserStoryRepository userStoryRepository;
    private final TasksRepository tasksRepository;
    private final SprintRepository sprintRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    @Transactional(readOnly = true)
    public int ingest(Long projectId) {
        int count = 0;

        List<UserStory> stories = projectId != null
                ? userStoryRepository.findByFeature_Capability_Project_Id(projectId)
                : userStoryRepository.findAll();

        for (UserStory s : stories) {
            Long pid = projectId != null ? projectId : resolveStoryProjectId(s);
            String text = formatUserStory(s);
            float[] vector = embeddingService.embed(text);
            if (vector != null) {
                vectorStoreService.upsert("user_story", s.getId(), pid, vector, text);
                count++;
            }
        }

        List<Tasks> tasks = projectId != null
                ? tasksRepository.findByProject_Id(projectId)
                : tasksRepository.findAll();

        for (Tasks t : tasks) {
            Long pid = t.getProject() != null ? t.getProject().getId() : null;
            String text = formatTask(t);
            float[] vector = embeddingService.embed(text);
            if (vector != null) {
                vectorStoreService.upsert("task", t.getId(), pid, vector, text);
                count++;
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

    private Long resolveStoryProjectId(UserStory s) {
        try {
            return s.getFeature().getCapability().getProject().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatUserStory(UserStory s) {
        return "[UserStory #" + s.getId() + "] " + s.getTitle() +
                "\nEstado: " + nvl(s.getStatus()) +
                " | Prioridad: " + nvl(s.getPriority()) +
                " | Puntos: " + nvl(s.getStoryPoints()) +
                "\nDescripción: " + nvl(s.getDescription()) +
                "\nCriterios de aceptación: " + nvl(s.getAcceptanceCriteria());
    }

    private String formatTask(Tasks t) {
        return "[Task #" + t.getId() + "] " + t.getTitle() +
                "\nEstado: " + nvl(t.getStatus()) +
                " | Prioridad: " + nvl(t.getPriority()) +
                " | Horas estimadas: " + nvl(t.getEstimatedHours()) +
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
