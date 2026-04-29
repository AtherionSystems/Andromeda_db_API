package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CapabilityRepository extends JpaRepository<Capability, Long> {
    List<Capability> findByProject_Id(Long projectId);
}
