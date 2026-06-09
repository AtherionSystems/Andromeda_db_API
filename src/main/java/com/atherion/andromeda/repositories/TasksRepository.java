// TasksRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.dto.SprintTaskAssigneeRow;
import com.atherion.andromeda.dto.TaskResponse;
import com.atherion.andromeda.model.Tasks;
import com.atherion.andromeda.projections.TaskDistributionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TasksRepository extends JpaRepository<Tasks, Long> {

    List<Tasks> findByProject_Id(Long projectId);
    List<Tasks> findByUserStoryIdIn(List<Long> userStoryIds);
    List<Tasks> findByProject_IdAndUserStoryId(Long projectId, Long userStoryId);
    long countByProject_Id(Long projectId);
    long countByUserStoryId(Long userStoryId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE user_story_id IN (SELECT id FROM user_stories WHERE feature_id = :featureId)", nativeQuery = true)
    long countByUserStoryFeatureId(@Param("featureId") Long featureId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE user_story_id IN (SELECT us.id FROM user_stories us JOIN features f ON us.feature_id = f.id WHERE f.capability_id = :capabilityId)", nativeQuery = true)
    long countByUserStoryCapabilityId(@Param("capabilityId") Long capabilityId);
    List<Tasks> findByProject_IdAndStatus(Long projectId, String status);

    @Query("SELECT t FROM Tasks t JOIN FETCH t.project WHERE t.project.id = :projectId")
    List<Tasks> findByProjectIdFetched(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Tasks t JOIN FETCH t.project WHERE t.project.id = :projectId AND t.userStoryId = :userStoryId")
    List<Tasks> findByProjectIdAndUserStoryIdFetched(@Param("projectId") Long projectId, @Param("userStoryId") Long userStoryId);

    @Query("SELECT t FROM Tasks t JOIN FETCH t.project WHERE t.project.id = :projectId AND t.status = :status")
    List<Tasks> findByProjectIdAndStatusFetched(@Param("projectId") Long projectId, @Param("status") String status);

    @Query("""
            SELECT new com.atherion.andromeda.dto.TaskResponse(
                t.id, t.title, t.description, t.priority, t.status,
                t.startDate, t.dueDate, t.actualEnd, t.estimatedHours, t.actualHours,
                t.userStoryId, t.project.name, u.name
            )
            FROM TaskAssignment ta
            JOIN ta.task t
            JOIN ta.user u
            WHERE t.project.id = :projectId
              AND u.id = :userId
            """)
    List<TaskResponse> findByProjectIdAndAssignedUserIdAsResponse(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.TaskResponse(
                t.id, t.title, t.description, t.priority, t.status,
                t.startDate, t.dueDate, t.actualEnd, t.estimatedHours, t.actualHours,
                t.userStoryId, t.project.name, u.name
            )
            FROM TaskAssignment ta
            JOIN ta.task t
            JOIN ta.user u
            WHERE u.id = :userId
              AND (:projectId IS NULL OR t.project.id = :projectId)
              AND (:status IS NULL OR t.status = :status)
            """)
    List<TaskResponse> findByAssignedUserIdAsResponse(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("status") String status);

    @Query("""
            SELECT new com.atherion.andromeda.dto.SprintTaskAssigneeRow(
                t.userStoryId, t.id, t.title, t.priority, t.status, t.dueDate,
                t.estimatedHours, t.actualHours,
                u.id, u.name
            )
            FROM Tasks t
            LEFT JOIN TaskAssignment ta ON ta.task = t
            LEFT JOIN ta.user u
            WHERE t.userStoryId IN :storyIds
            """)
    List<SprintTaskAssigneeRow> findTasksWithAssigneesByStoryIds(@Param("storyIds") List<Long> storyIds);

    @Query("""
    SELECT t.status AS status,
           COUNT(t.id) AS total
    FROM Tasks t
    WHERE t.project.id = :projectId
    GROUP BY t.status
    """)
    List<TaskDistributionProjection> getTaskDistributionByProject(
            @Param("projectId") Long projectId
    );
}
