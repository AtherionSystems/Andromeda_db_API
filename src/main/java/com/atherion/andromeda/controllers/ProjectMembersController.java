package com.atherion.andromeda.controllers;

import com.atherion.andromeda.dto.CreateProjectMemberRequest;
import com.atherion.andromeda.dto.ProjectMemberResponse;
import com.atherion.andromeda.dto.UpdateProjectMemberRequest;
import com.atherion.andromeda.model.Project;
import com.atherion.andromeda.model.ProjectMember;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.services.ProjectMemberService;
import com.atherion.andromeda.services.ProjectService;
import com.atherion.andromeda.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project-members")
@RequiredArgsConstructor
public class ProjectMembersController {

	private final ProjectMemberService projectMemberService;
	private final ProjectService projectService;
	private final UserService userService;

	// GET /api/project-members?projectId=1&userId=2
	@GetMapping
	public ResponseEntity<List<ProjectMemberResponse>> getAll(
			@RequestParam(required = false) Long projectId,
			@RequestParam(required = false) Long userId) {
		List<ProjectMember> members;

		if (projectId != null) {
			members = projectMemberService.findByProjectId(projectId);
		} else if (userId != null) {
			members = projectMemberService.findByUserId(userId);
		} else {
			members = projectMemberService.findAll();
		}

		return ResponseEntity.ok(members.stream().map(ProjectMemberResponse::from).toList());
	}

	// GET /api/project-members/{id}
	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable Long id) {
		return projectMemberService.findById(id)
				.<ResponseEntity<?>>map(member -> ResponseEntity.ok(ProjectMemberResponse.from(member)))
				.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "Project member not found")));
	}

	// POST /api/project-members
	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody CreateProjectMemberRequest req) {
		Project project = projectService.findById(req.projectId()).orElse(null);
		if (project == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Project not found"));
		}

		User user = userService.findById(req.userId()).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "User not found"));
		}

		if (projectMemberService.existsByProjectIdAndUserId(project.getId(), user.getId())) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("error", "User is already a member of this project"));
		}

		ProjectMember member = new ProjectMember();
		member.setProject(project);
		member.setUser(user);
		if (req.role() != null && !req.role().isBlank()) {
			member.setRole(req.role());
		}

		ProjectMember saved = projectMemberService.save(member);
		return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMemberResponse.from(saved));
	}

	// PUT /api/project-members/{id}
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id,
									@Valid @RequestBody UpdateProjectMemberRequest req) {
		ProjectMember member = projectMemberService.findById(id).orElse(null);
		if (member == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Project member not found"));
		}

		Long currentProjectId = member.getProject().getId();
		Long currentUserId = member.getUser().getId();
		Long targetProjectId = req.projectId() != null ? req.projectId() : member.getProject().getId();
		Long targetUserId = req.userId() != null ? req.userId() : member.getUser().getId();

		boolean changedRelation = !targetProjectId.equals(currentProjectId)
				|| !targetUserId.equals(currentUserId);
		if (changedRelation && projectMemberService.existsByProjectIdAndUserId(targetProjectId, targetUserId)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("error", "User is already a member of this project"));
		}

		if (req.projectId() != null) {
			Project project = projectService.findById(req.projectId()).orElse(null);
			if (project == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "Project not found"));
			}
			member.setProject(project);
		}

		if (req.userId() != null) {
			User user = userService.findById(req.userId()).orElse(null);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("error", "User not found"));
			}
			member.setUser(user);
		}

		if (req.role() != null && !req.role().isBlank()) {
			member.setRole(req.role());
		}

		return ResponseEntity.ok(ProjectMemberResponse.from(projectMemberService.save(member)));
	}

	// DELETE /api/project-members/{id}
	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		if (projectMemberService.findById(id).isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Project member not found"));
		}
		projectMemberService.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
