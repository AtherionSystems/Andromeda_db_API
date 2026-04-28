package com.atherion.andromeda.services;

import com.atherion.andromeda.model.Feature;
import com.atherion.andromeda.repositories.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeatureService {
    private final FeatureRepository featureRepository;

    public List<Feature> findAll() { return featureRepository.findAll(); }
    public List<Feature> findByProjectId(Long projectId) { return featureRepository.findByCapability_Project_Id(projectId); }
    public List<Feature> findByCapabilityId(Long capabilityId) { return featureRepository.findByCapability_Id(capabilityId); }
    public Optional<Feature> findById(Long id) { return featureRepository.findById(id); }
    public Feature save(Feature feature) { return featureRepository.save(feature); }
    public void deleteById(Long id) { featureRepository.deleteById(id); }
}
