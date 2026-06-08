package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.repositories.FeatureRepository;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeatureService {
    private final FeatureRepository featureRepository;
    private final UserStoryRepository userStoryRepository;
    private final RagIngestionService ragIngestionService;

    public List<Feature> findAll() { return featureRepository.findAll(); }
    public List<Feature> findByProjectId(Long projectId) { return featureRepository.findByCapability_Project_Id(projectId); }
    public List<Feature> findByCapabilityId(Long capabilityId) { return featureRepository.findByCapability_Id(capabilityId); }
    public Optional<Feature> findById(Long id) { return featureRepository.findById(id); }
    public Feature save(Feature feature) { return featureRepository.save(feature); }

    public void deleteById(Long id) {
        userStoryRepository.findByFeature_Id(id)
                .forEach(s -> ragIngestionService.deleteAsync("user_story", s.getId()));
        featureRepository.deleteById(id);
    }
}
