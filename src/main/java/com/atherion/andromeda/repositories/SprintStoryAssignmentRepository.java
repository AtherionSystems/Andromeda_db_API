package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.projections.BurndownProjection;
import com.atherion.andromeda.projections.HoursPerUserProjection;
import com.atherion.andromeda.projections.TeamVelocityProjection;
import com.atherion.andromeda.projections.UserTasksPerSprintProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintStoryAssignmentRepository extends JpaRepository<SprintStoryAssignment, Long> {
    List<SprintStoryAssignment> findBySprint_Id(Long sprintId);
    boolean existsBySprint_IdAndUserStoryIdAndIsActiveAndRemovedAtIsNull(
            Long sprintId,
            Long userStoryId,
            Integer isActive
    );
    List<SprintStoryAssignment> findBySprint_IdInAndIsActiveAndRemovedAtIsNull(List<Long> sprintIds, Integer isActive);
}