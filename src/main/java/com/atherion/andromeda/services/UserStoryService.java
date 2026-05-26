package com.atherion.andromeda.services;

import com.atherion.andromeda.dto.AssignedUserSummary;
import com.atherion.andromeda.dto.StoryAssigneeRow;
import com.atherion.andromeda.dto.UserStoryResponse;
import com.atherion.andromeda.model.UserStory;
import com.atherion.andromeda.repositories.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public List<UserStoryResponse> findByFeatureIdAsResponse(Long featureId) {
        return enrichWithAssignees(userStoryRepository.findByFeatureIdAsResponse(featureId));
    }

    public List<UserStoryResponse> findByProjectIdAsResponse(Long projectId) {
        return enrichWithAssignees(userStoryRepository.findByProjectIdAsResponse(projectId));
    }

    public Optional<UserStoryResponse> findByIdAsResponse(Long id) {
        return userStoryRepository.findByIdAsResponse(id)
                .map(s -> enrichWithAssignees(List.of(s)).get(0));
    }

    private List<UserStoryResponse> enrichWithAssignees(List<UserStoryResponse> stories) {
        if (stories.isEmpty()) return stories;

        List<Long> ids = stories.stream().map(UserStoryResponse::id).toList();

        Map<Long, List<AssignedUserSummary>> byStory = userStoryRepository
                .findAssigneesByStoryIds(ids)
                .stream()
                .collect(Collectors.groupingBy(
                        StoryAssigneeRow::storyId,
                        Collectors.mapping(
                                r -> new AssignedUserSummary(r.userId(), r.userName()),
                                Collectors.toList()
                        )
                ));

        return stories.stream()
                .map(s -> s.withAssignedUsers(byStory.getOrDefault(s.id(), List.of())))
                .toList();
    }
}
