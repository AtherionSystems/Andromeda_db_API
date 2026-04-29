package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.StorySpillover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorySpilloverRepository extends JpaRepository<StorySpillover, Long> {
    List<StorySpillover> findByUserStory_Id(Long userStoryId);
    List<StorySpillover> findByOriginSprint_Id(Long originSprintId);
    List<StorySpillover> findByDestinationSprint_Id(Long destinationSprintId);
}
