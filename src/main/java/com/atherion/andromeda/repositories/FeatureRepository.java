package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {
    List<Feature> findByCapability_Id(Long capabilityId);
    List<Feature> findByCapability_Project_Id(Long projectId);
}
