package com.atherion.andromeda.services;

import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStoryService {
    private final UserStoryRepository userStoryRepository;

    public List<UserStory> findAll() { return userStoryRepository.findAll(); }
    public List<UserStory> findByProjectId(Long projectId) { return userStoryRepository.findByFeature_Capability_Project_Id(projectId); }
    public List<UserStory> findByFeatureId(Long featureId) { return userStoryRepository.findByFeature_Id(featureId); }
    public List<UserStory> findByOwnerId(Long ownerId) { return userStoryRepository.findByOwner_Id(ownerId); }
    public Optional<UserStory> findById(Long id) { return userStoryRepository.findById(id); }
    public UserStory save(UserStory userStory) { return userStoryRepository.save(userStory); }
    public void deleteById(Long id) { userStoryRepository.deleteById(id); }
}
