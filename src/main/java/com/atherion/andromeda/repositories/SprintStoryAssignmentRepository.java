package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.SprintStoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // KPI 1: Tasa de finalización por sprint
    @Query("""
        SELECT s.name AS sprintName,
               COUNT(ssa.id) AS totalStories,
               SUM(CASE WHEN us.status = 'done' THEN 1 ELSE 0 END) AS completedStories
        FROM SprintStoryAssignment ssa
        JOIN ssa.sprint s
        JOIN ssa.userStory us
        WHERE s.project.id = :projectId
        GROUP BY s.id, s.name, s.startDate
        ORDER BY s.startDate
        """)
    List<CompletionRateProjection> getCompletionRateByProject(
            @Param("projectId") Long projectId
    );

    // KPI 2: Velocidad del equipo (story points por sprint)
    @Query("""
        SELECT s.name AS sprintName,
               SUM(CASE WHEN us.status = 'done' THEN us.storyPoints ELSE 0 END) AS pointsCompleted,
               SUM(us.storyPoints) AS pointsPlanned
        FROM SprintStoryAssignment ssa
        JOIN ssa.sprint s
        JOIN ssa.userStory us
        WHERE s.project.id = :projectId
        GROUP BY s.id, s.name, s.startDate
        ORDER BY s.startDate
        """)
    List<TeamVelocityProjection> getTeamVelocityByProject(
            @Param("projectId") Long projectId
    );

    // KPI 4: Tasks completadas por usuario por sprint
    @Query("""
        SELECT s.name AS sprintName,
               u.name AS userName,
               COUNT(t.id) AS tasksCompleted
        FROM SprintStoryAssignment ssa
        JOIN ssa.sprint s
        JOIN ssa.userStory us
        JOIN us.tasks t
        JOIN t.taskAssignments ta
        JOIN ta.user u
        WHERE s.project.id = :projectId
          AND t.status     = 'done'
        GROUP BY s.id, s.name, s.startDate, u.name
        ORDER BY s.startDate
        """)
    List<UserTasksPerSprintProjection> getUserTasksPerSprint(
            @Param("projectId") Long projectId
    );
}
