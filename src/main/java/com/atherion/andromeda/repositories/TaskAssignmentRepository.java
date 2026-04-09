// TaskAssignmentRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {}