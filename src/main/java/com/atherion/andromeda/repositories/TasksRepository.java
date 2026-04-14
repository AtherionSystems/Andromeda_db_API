// TasksRepository.java
package com.atherion.andromeda.repositories;
  
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
 
import com.atherion.andromeda.model.Tasks;

public interface TasksRepository extends JpaRepository<Tasks, Long> {
    List<Tasks> findByProjectId(Long projectId);
}