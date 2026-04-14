// TaskAssignmentRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    List<TaskAssignment> findByTask_Id(Long taskId);

    Optional<TaskAssignment> findByTask_IdAndUser_Id(Long taskId, Long userId);
}