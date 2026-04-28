package com.atherion.andromeda.services;

import com.atherion.andromeda.model.SprintRetrospective;
import com.atherion.andromeda.repositories.SprintRetrospectiveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SprintRetrospectiveService {
    private final SprintRetrospectiveRepository sprintRetrospectiveRepository;

    public List<SprintRetrospective> findAll() { return sprintRetrospectiveRepository.findAll(); }
    public Optional<SprintRetrospective> findBySprintId(Long sprintId) { return sprintRetrospectiveRepository.findBySprint_Id(sprintId); }
    public Optional<SprintRetrospective> findById(Long id) { return sprintRetrospectiveRepository.findById(id); }
    public SprintRetrospective save(SprintRetrospective sprintRetrospective) { return sprintRetrospectiveRepository.save(sprintRetrospective); }
    public void deleteById(Long id) { sprintRetrospectiveRepository.deleteById(id); }
}
