// TaskAssignmentRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTask_Id(Long taskId);
    List<TaskAssignment> findByTask_IdIn(List<Long> taskIds);

    Optional<TaskAssignment> findByTask_IdAndUser_Id(Long taskId, Long userId);

    @Query("""
            SELECT ta FROM TaskAssignment ta
            JOIN FETCH ta.task t
            JOIN FETCH ta.user u
            WHERE t.project.id = :projectId
            """)
    List<TaskAssignment> findAllByProjectIdWithDetails(@Param("projectId") Long projectId);
}
