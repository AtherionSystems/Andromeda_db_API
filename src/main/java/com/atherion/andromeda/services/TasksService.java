// TasksService.java
package com.atherion.andromeda.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.repositories.TasksRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TasksService {
    private final TasksRepository tasksRepository;

    public List<Tasks> findByProjectId(Long projectId) {
        return tasksRepository.findByProjectId(projectId);
    }

    public List<Tasks> findAll() { return tasksRepository.findAll(); }
    public Optional<Tasks> findById(Long id) { return tasksRepository.findById(id); }
    public Tasks save(Tasks task) { return tasksRepository.save(task); }
    public void deleteById(Long id) { tasksRepository.deleteById(id); }
}