// ProjectMemberService.java
package com.atherion.andromeda.services;

import com.atherion.andromeda.model.ProjectMember;
import com.atherion.andromeda.repositories.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;

    public List<ProjectMember> findAll() { return projectMemberRepository.findAll(); }
    public List<ProjectMember> findByProjectId(Long projectId) { return projectMemberRepository.findByProject_Id(projectId); }
    public List<ProjectMember> findByUserId(Long userId) { return projectMemberRepository.findByUser_Id(userId); }
    public Optional<ProjectMember> findById(Long id) { return projectMemberRepository.findById(id); }
    public boolean existsByProjectIdAndUserId(Long projectId, Long userId) {
        return projectMemberRepository.existsByProject_IdAndUser_Id(projectId, userId);
    }
    public ProjectMember save(ProjectMember projectMember) { return projectMemberRepository.save(projectMember); }
    public void deleteById(Long id) { projectMemberRepository.deleteById(id); }
}