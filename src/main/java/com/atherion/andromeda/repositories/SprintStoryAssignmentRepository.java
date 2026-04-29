package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.SprintStoryAssignment;
import com.atherion.andromeda.projections.CompletionRateProjection;
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

    @Query(value = """
    SELECT s.name AS sprintName,
           COUNT(ssa.id) AS totalStories,
           SUM(CASE WHEN us.status = 'done' THEN 1 ELSE 0 END) AS completedStories
    FROM sprint_stories_assignments ssa
    JOIN sprints s       ON ssa.sprint_id     = s.id
    JOIN user_stories us ON ssa.user_story_id = us.id
    WHERE s.project_id = :projectId
    GROUP BY s.id, s.name, s.start_date
    ORDER BY s.start_date
    """, nativeQuery = true)
    List<CompletionRateProjection> getCompletionRateByProject(@Param("projectId") Long projectId);

    @Query(value = """
    SELECT s.name AS sprintName,
           SUM(CASE WHEN us.status = 'done' THEN us.story_points ELSE 0 END) AS pointsCompleted,
           SUM(us.story_points) AS pointsPlanned
    FROM sprint_stories_assignments ssa
    JOIN sprints s       ON ssa.sprint_id     = s.id
    JOIN user_stories us ON ssa.user_story_id = us.id
    WHERE s.project_id = :projectId
    GROUP BY s.id, s.name, s.start_date
    ORDER BY s.start_date
    """, nativeQuery = true)
    List<TeamVelocityProjection> getTeamVelocityByProject(@Param("projectId") Long projectId);

    @Query(value = """
    SELECT s.name AS sprintName,
           u.name AS userName,
           COUNT(t.id) AS tasksCompleted
    FROM sprint_stories_assignments ssa
    JOIN sprints s       ON ssa.sprint_id      = s.id
    JOIN user_stories us ON ssa.user_story_id  = us.id
    JOIN tasks t         ON t.user_story_id    = us.id
    JOIN task_assignments ta ON ta.task_id     = t.id
    JOIN users u         ON ta.user_id         = u.id
    WHERE s.project_id = :projectId
      AND t.status     = 'done'
    GROUP BY s.name, u.name, s.start_date
    ORDER BY s.start_date
    """, nativeQuery = true)
    List<UserTasksPerSprintProjection> getUserTasksPerSprint(@Param("projectId") Long projectId);
}