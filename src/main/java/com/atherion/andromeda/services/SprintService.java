package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.SprintResponse;
import com.atherion.andromeda.model.Sprint;
import com.atherion.andromeda.repositories.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SprintService {
    private final SprintRepository sprintRepository;

    public List<Sprint> findAll() { return sprintRepository.findAll(); }
    public List<Sprint> findByProjectId(Long projectId) { return sprintRepository.findByProject_Id(projectId); }
    public Optional<Sprint> findById(Long id) { return sprintRepository.findById(id); }
    public Sprint save(Sprint sprint) { return sprintRepository.save(sprint); }
    public void deleteById(Long id) { sprintRepository.deleteById(id); }

    public List<SprintResponse> findByProjectIdAsResponse(Long projectId) {
        return sprintRepository.findByProjectIdAsResponse(projectId);
    }

    public Optional<SprintResponse> findByIdAsResponse(Long id) {
        return sprintRepository.findByIdAsResponse(id);
    }
}