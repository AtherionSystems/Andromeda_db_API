// SprintTaskRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.dto.SprintTaskRow;
import com.atherion.andromeda.model.SprintTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintTaskRepository extends JpaRepository<SprintTask, Long> {

    List<SprintTask> findBySprint_Id(Long sprintId);

    @Query("SELECT COUNT(st) > 0 FROM SprintTask st " +
           "WHERE st.sprint.id = :sprintId AND st.task.id = :taskId AND st.removedAt IS NULL")
    boolean isTaskActiveInSprint(@Param("sprintId") Long sprintId, @Param("taskId") Long taskId);

    /**
     * Sprint task board for the last 2 sprints of a project.
     * Returns one row per task; multiple assignees are aggregated with LISTAGG.
     * Ordered: newest sprint first, then by status (in_progress → review → todo → done),
     * then by priority (critical → high → medium → low).
     */
    @Query(nativeQuery = true, value = """
            SELECT
                t.id              AS id,
                t.title           AS title,
                t.status          AS status,
                t.priority        AS priority,
                t.story_points    AS storyPoints,
                t.estimated_hours AS estimatedHours,
                t.actual_hours    AS actualHours,
                LISTAGG(u.username, ', ') WITHIN GROUP (ORDER BY u.username) AS assignees,
                s.name            AS sprintName
            FROM sprint_tasks st
            JOIN tasks   t  ON t.id  = st.task_id
            JOIN sprints s  ON s.id  = st.sprint_id
            LEFT JOIN task_assignments ta ON ta.task_id = t.id
            LEFT JOIN users            u  ON u.id       = ta.user_id
            WHERE st.sprint_id IN (
                SELECT id FROM (
                    SELECT id FROM sprints
                    WHERE  project_id = :projectId
                    ORDER  BY created_at DESC
                    FETCH  FIRST 2 ROWS ONLY
                )
            )
            AND st.removed_at IS NULL
            GROUP BY t.id, t.title, t.status, t.priority,
                     t.story_points, t.estimated_hours, t.actual_hours,
                     s.id, s.name
            ORDER BY s.id DESC,
                CASE t.status
                    WHEN 'in_progress' THEN 1
                    WHEN 'review'      THEN 2
                    WHEN 'todo'        THEN 3
                    WHEN 'done'        THEN 4
                    ELSE 5
                END,
                CASE t.priority
                    WHEN 'critical' THEN 1
                    WHEN 'high'     THEN 2
                    WHEN 'medium'   THEN 3
                    WHEN 'low'      THEN 4
                    ELSE 5
                END
            """)
    List<SprintTaskRow> findSprintBoard(@Param("projectId") Long projectId);
}
