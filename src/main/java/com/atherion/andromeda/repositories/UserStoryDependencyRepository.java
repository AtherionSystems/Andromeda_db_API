package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.UserStoryDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStoryDependencyRepository extends JpaRepository<UserStoryDependency, Long> {
    List<UserStoryDependency> findByStory_Id(Long storyId);
    List<UserStoryDependency> findByBlockedBy_Id(Long blockedById);
}
