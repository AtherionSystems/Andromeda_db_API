package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {
    List<UserStory> findByFeature_Id(Long featureId);
    List<UserStory> findByOwner_Id(Long ownerId);
    List<UserStory> findByFeature_Capability_Project_Id(Long projectId);
}
