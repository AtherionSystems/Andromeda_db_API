package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.SprintRetrospective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SprintRetrospectiveRepository extends JpaRepository<SprintRetrospective, Long> {
    Optional<SprintRetrospective> findBySprint_Id(Long sprintId);
}
