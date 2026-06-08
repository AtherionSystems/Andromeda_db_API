package com.atherion.andromeda.repositories;

import com.atherion.andromeda.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);
	List<ProjectMember> findByProject_Id(Long projectId);
	List<ProjectMember> findByUser_Id(Long userId);
	long countByProject_Id(Long projectId);

	@Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.user WHERE pm.project.id = :projectId")
	List<ProjectMember> findByProjectIdWithUser(@Param("projectId") Long projectId);
}