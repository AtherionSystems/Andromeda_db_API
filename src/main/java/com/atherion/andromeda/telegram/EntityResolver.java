package com.atherion.andromeda.telegram;

import com.atherion.andromeda.model.*;
import com.atherion.andromeda.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves entity names to IDs and builds entity lists for AI context injection.
 * Used by AiIntentRouter for Phase 2 name resolution.
 */
@Component
@RequiredArgsConstructor
public class EntityResolver {

    private final ProjectService    projectService;
    private final CapabilityService capabilityService;
    private final FeatureService    featureService;
    private final TasksService      tasksService;
    private final UserStoryService  userStoryService;

    // ── AI context builders ───────────────────────────────────────────────────

    /** Compact project list for AI prompts: "[1] Alpha (active), [2] Beta (paused)" */
    public String buildProjectList() {
        List<Project> projects = projectService.findAll();
        if (projects.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Known projects: ");
        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            sb.append("[").append(p.getId()).append("] ").append(p.getName());
            if (p.getStatus() != null) sb.append(" (").append(p.getStatus()).append(")");
            if (i < projects.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /** Compact capability list for the active project: "[5] Authentication, [6] Reporting" */
    public String buildCapabilityList(Long projectId) {
        if (projectId == null) return "";
        List<Capability> caps = capabilityService.findByProjectId(projectId);
        if (caps.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Known capabilities in active project: ");
        for (int i = 0; i < caps.size(); i++) {
            Capability c = caps.get(i);
            sb.append("[").append(c.getId()).append("] ").append(c.getName());
            if (i < caps.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /** Compact feature list for the active capability: "[8] Login Flow, [9] OAuth" */
    public String buildFeatureList(Long capabilityId) {
        if (capabilityId == null) return "";
        List<Feature> features = featureService.findByCapabilityId(capabilityId);
        if (features.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Known features in active capability: ");
        for (int i = 0; i < features.size(); i++) {
            Feature f = features.get(i);
            sb.append("[").append(f.getId()).append("] ").append(f.getName());
            if (i < features.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /** Compact user story list for the active feature: "[12] As a user I can login, [13] Reset password" */
    public String buildUserStoryList(Long featureId) {
        if (featureId == null) return "";
        List<UserStory> stories = userStoryService.findByFeatureId(featureId);
        if (stories.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Known user stories in active feature: ");
        for (int i = 0; i < stories.size(); i++) {
            UserStory s = stories.get(i);
            sb.append("[").append(s.getId()).append("] ").append(s.getTitle());
            if (i < stories.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // ── name → ID resolvers ───────────────────────────────────────────────────

    /** Case-insensitive substring match on project name. */
    public Optional<Long> resolveProjectByName(String name) {
        String lower = name.toLowerCase();
        return projectService.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name)
                          || p.getName().toLowerCase().contains(lower))
                .map(Project::getId)
                .findFirst();
    }

    /** Case-insensitive capability name match, scoped to projectId when provided. */
    public Optional<Long> resolveCapabilityByName(String name, Long projectId) {
        List<Capability> caps = projectId != null
                ? capabilityService.findByProjectId(projectId)
                : capabilityService.findAll();
        String lower = name.toLowerCase();
        return caps.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name)
                          || c.getName().toLowerCase().contains(lower))
                .map(Capability::getId)
                .findFirst();
    }

    /** Case-insensitive feature name match, scoped to capabilityId when provided. */
    public Optional<Long> resolveFeatureByName(String name, Long capabilityId) {
        List<Feature> features = capabilityId != null
                ? featureService.findByCapabilityId(capabilityId)
                : featureService.findAll();
        String lower = name.toLowerCase();
        return features.stream()
                .filter(f -> f.getName().equalsIgnoreCase(name)
                          || f.getName().toLowerCase().contains(lower))
                .map(Feature::getId)
                .findFirst();
    }

    /** Case-insensitive user story title match, scoped to featureId when provided. */
    public Optional<Long> resolveUserStoryByTitle(String title, Long featureId) {
        List<UserStory> stories = featureId != null
                ? userStoryService.findByFeatureId(featureId)
                : userStoryService.findAll();
        String lower = title.toLowerCase();
        return stories.stream()
                .filter(s -> s.getTitle().equalsIgnoreCase(title)
                          || s.getTitle().toLowerCase().contains(lower))
                .map(UserStory::getId)
                .findFirst();
    }

    /** Case-insensitive task title match, scoped to projectId when provided. */
    public Optional<Long> resolveTaskByTitle(String title, Long projectId) {
        List<Tasks> tasks = projectId != null
                ? tasksService.findByProjectId(projectId)
                : tasksService.findAll();
        String lower = title.toLowerCase();
        return tasks.stream()
                .filter(t -> t.getTitle().equalsIgnoreCase(title)
                          || t.getTitle().toLowerCase().contains(lower))
                .map(Tasks::getId)
                .findFirst();
    }
}
