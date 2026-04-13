// ProjectRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {}