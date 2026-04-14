// TasksService.java
package com.atherion.andromeda.services;

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
    public Optional<Tasks> findById(Long id) { return tasksRepository.findById(id); }
    public Tasks save(Tasks task) { return tasksRepository.save(task); }
    public void deleteById(Long id) { tasksRepository.deleteById(id); }
}