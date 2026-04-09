// SprintTaskRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.SprintTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SprintTaskRepository extends JpaRepository<SprintTask, Long> {}