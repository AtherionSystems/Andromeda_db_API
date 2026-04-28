// TasksRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Tasks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TasksRepository extends JpaRepository<Tasks, Long> {

    List<Tasks> findByProject_Id(Long projectId);
    List<Tasks> findByUserStoryIdIn(List<Long> userStoryIds);
}
