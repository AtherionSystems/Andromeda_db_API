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
    public Optional<ProjectMember> findById(Long id) { return projectMemberRepository.findById(id); }
    public ProjectMember save(ProjectMember projectMember) { return projectMemberRepository.save(projectMember); }
    public void deleteById(Long id) { projectMemberRepository.deleteById(id); }
}