// TasksService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.TaskResponse;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.repositories.TasksRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;

    public List<Tasks> findAll() { return tasksRepository.findAll(); }
    public List<Tasks> findByProjectId(Long projectId) { return tasksRepository.findByProject_Id(projectId); }
    public List<Tasks> findByUserStoryIds(List<Long> userStoryIds) { return tasksRepository.findByUserStoryIdIn(userStoryIds); }
    public List<Tasks> findByProjectIdAndUserStoryId(Long projectId, Long userStoryId) { return tasksRepository.findByProject_IdAndUserStoryId(projectId, userStoryId); }
    public List<Tasks> findByProjectIdAndStatus(Long projectId, String status) { return tasksRepository.findByProject_IdAndStatus(projectId, status); }
    public Optional<Tasks> findById(Long id) { return tasksRepository.findById(id); }
    public Tasks save(Tasks task) { return tasksRepository.save(task); }
    public void deleteById(Long id) { tasksRepository.deleteById(id); }

    public List<TaskResponse> findByProjectIdAsResponse(Long projectId) {
        return tasksRepository.findByProjectIdFetched(projectId).stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> findByProjectIdAndUserStoryIdAsResponse(Long projectId, Long userStoryId) {
        return tasksRepository.findByProjectIdAndUserStoryIdFetched(projectId, userStoryId).stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> findByProjectIdAndStatusAsResponse(Long projectId, String status) {
        return tasksRepository.findByProjectIdAndStatusFetched(projectId, status).stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> findByProjectIdAndAssignedUserIdAsResponse(Long projectId, Long userId) {
        return tasksRepository.findByProjectIdAndAssignedUserIdAsResponse(projectId, userId);
    }
}
