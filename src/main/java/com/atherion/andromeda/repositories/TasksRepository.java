// TasksRepository.java
package com.atherion.andromeda.repositories;

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
