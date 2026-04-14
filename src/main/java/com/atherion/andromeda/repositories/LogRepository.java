package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    @Query("""
            SELECT l
            FROM Log l
            LEFT JOIN l.user u
            WHERE (:userId IS NULL OR u.id = :userId)
              AND (:fromDate IS NULL OR l.logDate >= :fromDate)
              AND (:toDate IS NULL OR l.logDate <= :toDate)
              AND (:taskId IS NULL OR (LOWER(l.entity) = 'task' AND l.entityId = :taskId))
              AND (
                    :projectId IS NULL
                    OR (LOWER(l.entity) = 'project' AND l.entityId = :projectId)
                    OR (
                        LOWER(l.entity) = 'task'
                        AND EXISTS (
                            SELECT 1
                            FROM Tasks t
                            WHERE t.id = l.entityId
                              AND t.project.id = :projectId
                        )
                    )
              )
            ORDER BY l.logDate DESC
            """)
    List<Log> search(@Param("projectId") Long projectId,
                     @Param("taskId") Long taskId,
                     @Param("userId") Long userId,
                     @Param("fromDate") LocalDateTime fromDate,
                     @Param("toDate") LocalDateTime toDate);

    @Modifying
    @Transactional
    @Query("DELETE FROM Log l WHERE l.id = :id")
    void deleteByIdJpql(@Param("id") Long id);
}
