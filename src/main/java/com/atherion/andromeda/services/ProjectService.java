// ProjectService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    public List<Project> findAll() { return projectRepository.findAll(); }
    public Optional<Project> findById(Long id) { return projectRepository.findById(id); }
    public Project save(Project project) { return projectRepository.save(project); }
    public void deleteById(Long id) { projectRepository.deleteById(id); }
}