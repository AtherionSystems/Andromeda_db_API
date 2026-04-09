// ProjectMemberRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {}