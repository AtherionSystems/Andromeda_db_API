package com.atherion.andromeda.services;

import com.atherion.andromeda.model.ProjectMember;
import com.atherion.andromeda.repositories.ProjectMemberRepository;
import com.atherion.andromeda.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public List<ProjectMember> findAll() { return projectMemberRepository.findAll(); }
    public List<ProjectMember> findByProjectId(Long projectId) { return projectMemberRepository.findByProject_Id(projectId); }
    public List<ProjectMember> findByUserId(Long userId) { return projectMemberRepository.findByUser_Id(userId); }
    public Optional<ProjectMember> findById(Long id) { return projectMemberRepository.findById(id); }
    public Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId) {
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, userId);
    }
    public boolean existsByProjectIdAndUserId(Long projectId, Long userId) {
        return projectMemberRepository.existsByProject_IdAndUser_Id(projectId, userId);
    }
    public boolean isManagerOrOwner(Long projectId, Long userId) {
        var result = projectMemberRepository.findByProject_IdAndUser_Id(projectId, userId);
        return result.map(m -> "manager".equals(m.getRole()) || "owner".equals(m.getRole()))
                .orElse(false);
    }
    public boolean isManagerOrOwnerByIamSub(Long projectId, String iamSub) {
        return userRepository.findByIamSub(iamSub)
                .map(u -> isManagerOrOwner(projectId, u.getId()))
                .orElse(false);
    }
    public ProjectMember save(ProjectMember projectMember) { return projectMemberRepository.save(projectMember); }
    public void deleteById(Long id) { projectMemberRepository.deleteById(id); }
}