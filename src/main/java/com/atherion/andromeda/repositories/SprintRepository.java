package com.atherion.andromeda.repositories;

import com.atherion.andromeda.dto.SprintResponse;
import com.atherion.andromeda.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject_Id(Long projectId);
    List<Sprint> findTop2ByProject_IdOrderByCreatedAtDesc(Long projectId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.SprintResponse(
                s.id, s.name, s.goal, s.status, s.startDate, s.dueDate, s.actualEnd,
                s.createdAt, s.updatedAt, p.id, p.name
            )
            FROM Sprint s JOIN s.project p
            WHERE p.id = :projectId
            """)
    List<SprintResponse> findByProjectIdAsResponse(@Param("projectId") Long projectId);

    @Query("""
            SELECT new com.atherion.andromeda.dto.SprintResponse(
                s.id, s.name, s.goal, s.status, s.startDate, s.dueDate, s.actualEnd,
                s.createdAt, s.updatedAt, p.id, p.name
            )
            FROM Sprint s JOIN s.project p
            WHERE s.id = :id
            """)
    Optional<SprintResponse> findByIdAsResponse(@Param("id") Long id);
}
