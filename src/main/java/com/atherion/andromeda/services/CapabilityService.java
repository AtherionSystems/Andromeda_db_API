package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Capability;
import com.atherion.andromeda.repositories.CapabilityRepository;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CapabilityService {
    private final CapabilityRepository capabilityRepository;
    private final UserStoryRepository userStoryRepository;
    private final RagIngestionService ragIngestionService;

    public List<Capability> findAll() { return capabilityRepository.findAll(); }
    public List<Capability> findByProjectId(Long projectId) { return capabilityRepository.findByProject_Id(projectId); }
    public Optional<Capability> findById(Long id) { return capabilityRepository.findById(id); }
    public Capability save(Capability capability) { return capabilityRepository.save(capability); }

    public void deleteById(Long id) {
        userStoryRepository.findByFeature_Capability_Id(id)
                .forEach(s -> ragIngestionService.deleteAsync("user_story", s.getId()));
        capabilityRepository.deleteById(id);
    }
}
