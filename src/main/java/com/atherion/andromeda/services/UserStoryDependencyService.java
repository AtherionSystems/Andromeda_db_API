package com.atherion.andromeda.services;

import com.atherion.andromeda.model.UserStoryDependency;
import com.atherion.andromeda.repositories.UserStoryDependencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserStoryDependencyService {
    private final UserStoryDependencyRepository userStoryDependencyRepository;

    public List<UserStoryDependency> findAll() { return userStoryDependencyRepository.findAll(); }
    public List<UserStoryDependency> findByStoryId(Long storyId) { return userStoryDependencyRepository.findByStory_Id(storyId); }
    public List<UserStoryDependency> findByBlockedById(Long blockedById) { return userStoryDependencyRepository.findByBlockedBy_Id(blockedById); }
    public Optional<UserStoryDependency> findById(Long id) { return userStoryDependencyRepository.findById(id); }
    public UserStoryDependency save(UserStoryDependency dependency) { return userStoryDependencyRepository.save(dependency); }
    public void deleteById(Long id) { userStoryDependencyRepository.deleteById(id); }
}
