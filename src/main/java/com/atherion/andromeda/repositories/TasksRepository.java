// TasksRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Tasks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TasksRepository extends JpaRepository<Tasks, Long> {}