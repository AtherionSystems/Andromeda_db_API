// SprintTaskService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.SprintTask;
import com.atherion.andromeda.repositories.SprintTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SprintTaskService {
    private final SprintTaskRepository sprintTaskRepository;

    public List<SprintTask> findAll() { return sprintTaskRepository.findAll(); }
    public Optional<SprintTask> findById(Long id) { return sprintTaskRepository.findById(id); }
    public SprintTask save(SprintTask sprintTask) { return sprintTaskRepository.save(sprintTask); }
    public void deleteById(Long id) { sprintTaskRepository.deleteById(id); }
}