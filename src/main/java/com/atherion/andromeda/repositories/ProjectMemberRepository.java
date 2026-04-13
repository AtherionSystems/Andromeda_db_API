// ProjectMemberRepository.java
package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
	List<ProjectMember> findByProject_Id(Long projectId);
	List<ProjectMember> findByUser_Id(Long userId);
}
