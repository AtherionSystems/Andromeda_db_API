package com.atherion.andromeda.repositories;

import com.atherion.andromeda.dto.AssignedUserSummary;
import com.atherion.andromeda.dto.StoryAssigneeRow;
import com.atherion.andromeda.dto.UserStoryResponse;
import com.atherion.andromeda.dto.UserStorySummary;
import com.atherion.andromeda.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {
    List<UserStory> findByFeature_Id(Long featureId);
    List<UserStory> findByOwner_Id(Long ownerId);
    List<UserStory> findByFeature_Capability_Project_Id(Long projectId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.UserStoryResponse(
                us.id, us.title, us.description, us.acceptanceCriteria,
                us.priority, us.status, us.storyPoints,
                f.id, f.name,
                owner.name,
                cb.name,
                ub.name,
                us.createdAt, us.updatedAt
            )
            FROM UserStory us
            JOIN us.feature f
            LEFT JOIN us.owner owner
            JOIN us.createdBy cb
            LEFT JOIN us.updatedBy ub
            WHERE f.id = :featureId
            """)
    List<UserStoryResponse> findByFeatureIdAsResponse(@Param("featureId") Long featureId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.UserStoryResponse(
                us.id, us.title, us.description, us.acceptanceCriteria,
                us.priority, us.status, us.storyPoints,
                f.id, f.name,
                owner.name,
                cb.name,
                ub.name,
                us.createdAt, us.updatedAt
            )
            FROM UserStory us
            JOIN us.feature f
            JOIN f.capability c
            JOIN c.project p
            LEFT JOIN us.owner owner
            JOIN us.createdBy cb
            LEFT JOIN us.updatedBy ub
            WHERE p.id = :projectId
            """)
    List<UserStoryResponse> findByProjectIdAsResponse(@Param("projectId") Long projectId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.UserStoryResponse(
                us.id, us.title, us.description, us.acceptanceCriteria,
                us.priority, us.status, us.storyPoints,
                f.id, f.name,
                owner.name,
                cb.name,
                ub.name,
                us.createdAt, us.updatedAt
            )
            FROM UserStory us
            JOIN us.feature f
            LEFT JOIN us.owner owner
            JOIN us.createdBy cb
            LEFT JOIN us.updatedBy ub
            WHERE us.id = :id
            """)
    Optional<UserStoryResponse> findByIdAsResponse(@Param("id") Long id);

    @Query("""
            SELECT new com.atherion.andromeda.dto.UserStorySummary(
                us.id, us.title, us.priority, us.status, us.storyPoints, f.name, owner.name
            )
            FROM UserStory us
            JOIN us.feature f
            LEFT JOIN us.owner owner
            WHERE us.id IN :ids
            """)
    List<UserStorySummary> findSummariesByIds(@Param("ids") List<Long> ids);

    @Query("""
            SELECT DISTINCT new com.atherion.andromeda.dto.StoryAssigneeRow(
                t.userStoryId, u.id, u.name
            )
            FROM TaskAssignment ta
            JOIN ta.task t
            JOIN ta.user u
            WHERE t.userStoryId IN :storyIds
            """)
    List<StoryAssigneeRow> findAssigneesByStoryIds(@Param("storyIds") List<Long> storyIds);
}
