package com.atherion.andromeda.services;

import com.atherion.andromeda.model.StorySpillover;
import com.atherion.andromeda.repositories.StorySpilloverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorySpilloverService {
    private final StorySpilloverRepository storySpilloverRepository;

    public List<StorySpillover> findAll() { return storySpilloverRepository.findAll(); }
    public List<StorySpillover> findByUserStoryId(Long userStoryId) { return storySpilloverRepository.findByUserStory_Id(userStoryId); }
    public List<StorySpillover> findByOriginSprintId(Long originSprintId) { return storySpilloverRepository.findByOriginSprint_Id(originSprintId); }
    public List<StorySpillover> findByDestinationSprintId(Long destinationSprintId) { return storySpilloverRepository.findByDestinationSprint_Id(destinationSprintId); }
    public Optional<StorySpillover> findById(Long id) { return storySpilloverRepository.findById(id); }
    public StorySpillover save(StorySpillover spillover) { return storySpilloverRepository.save(spillover); }
    public void deleteById(Long id) { storySpilloverRepository.deleteById(id); }
}
