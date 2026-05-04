package com.atherion.andromeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, String> endpoints = new LinkedHashMap<>();

        endpoints.put("health",                      "GET    /health");
        endpoints.put("register",                    "POST   /api/auth/register");
        endpoints.put("login",                       "POST   /api/auth/login");
        endpoints.put("users",                       "GET    /api/users");
        endpoints.put("user",                        "GET    /api/users/{id}");
        endpoints.put("update-user",                 "PUT    /api/users/{id}");
        endpoints.put("delete-user",                 "DELETE /api/users/{id}");
        endpoints.put("projects",                    "GET    /api/projects");
        endpoints.put("project",                     "GET    /api/projects/{id}");
        endpoints.put("create-project",              "POST   /api/projects");
        endpoints.put("update-project",              "PATCH  /api/projects/{id}");
        endpoints.put("delete-project",              "DELETE /api/projects/{id}");
        endpoints.put("project-members",             "GET    /api/project-members");
        endpoints.put("project-member",              "GET    /api/project-members/{id}");
        endpoints.put("add-project-member",          "POST   /api/project-members");
        endpoints.put("update-project-member",       "PUT    /api/project-members/{id}");
        endpoints.put("remove-project-member",       "DELETE /api/project-members/{id}");
        endpoints.put("logs",                        "GET    /api/logs");
        endpoints.put("create-log",                  "POST   /api/logs");
        endpoints.put("project-logs",                "GET    /api/projects/{projectId}/logs");
        endpoints.put("ai-notify",                   "POST   /api/ai/notify");
        endpoints.put("ai-status",                   "GET    /api/ai/status");
        endpoints.put("capabilities",                "GET    /api/projects/{projectId}/capabilities");
        endpoints.put("capability",                  "GET    /api/projects/{projectId}/capabilities/{capabilityId}");
        endpoints.put("create-capability",           "POST   /api/projects/{projectId}/capabilities");
        endpoints.put("update-capability",           "PATCH  /api/projects/{projectId}/capabilities/{capabilityId}");
        endpoints.put("delete-capability",           "DELETE /api/projects/{projectId}/capabilities/{capabilityId}");
        endpoints.put("features",                    "GET    /api/projects/{projectId}/capabilities/{capabilityId}/features");
        endpoints.put("feature",                     "GET    /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}");
        endpoints.put("create-feature",              "POST   /api/projects/{projectId}/capabilities/{capabilityId}/features");
        endpoints.put("update-feature",              "PATCH  /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}");
        endpoints.put("delete-feature",              "DELETE /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}");
        endpoints.put("user-stories",                "GET    /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories");
        endpoints.put("user-story",                  "GET    /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}");
        endpoints.put("create-user-story",           "POST   /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories");
        endpoints.put("update-user-story",           "PATCH  /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}");
        endpoints.put("delete-user-story",           "DELETE /api/projects/{projectId}/capabilities/{capabilityId}/features/{featureId}/stories/{storyId}");
        endpoints.put("story-dependencies",          "GET    /api/projects/{projectId}/stories/{storyId}/dependencies");
        endpoints.put("story-dependency",            "GET    /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}");
        endpoints.put("create-story-dependency",     "POST   /api/projects/{projectId}/stories/{storyId}/dependencies");
        endpoints.put("update-story-dependency",     "PATCH  /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}");
        endpoints.put("delete-story-dependency",     "DELETE /api/projects/{projectId}/stories/{storyId}/dependencies/{dependencyId}");
        endpoints.put("tasks",                       "GET    /api/projects/{projectId}/tasks");
        endpoints.put("task",                        "GET    /api/projects/{projectId}/tasks/{taskId}");
        endpoints.put("create-task",                 "POST   /api/projects/{projectId}/tasks");
        endpoints.put("update-task",                 "PATCH  /api/projects/{projectId}/tasks/{taskId}");
        endpoints.put("delete-task",                 "DELETE /api/projects/{projectId}/tasks/{taskId}");
        endpoints.put("task-assignments",            "GET    /api/projects/{projectId}/tasks/{taskId}/assignments");
        endpoints.put("create-task-assignment",      "POST   /api/projects/{projectId}/tasks/{taskId}/assignments");
        endpoints.put("delete-task-assignment",      "DELETE /api/projects/{projectId}/tasks/{taskId}/assignments/{userId}");
        endpoints.put("sprints",                     "GET    /api/projects/{projectId}/sprints");
        endpoints.put("sprint",                      "GET    /api/projects/{projectId}/sprints/{sprintId}");
        endpoints.put("create-sprint",               "POST   /api/projects/{projectId}/sprints");
        endpoints.put("update-sprint",               "PATCH  /api/projects/{projectId}/sprints/{sprintId}");
        endpoints.put("delete-sprint",               "DELETE /api/projects/{projectId}/sprints/{sprintId}");
        endpoints.put("sprint-tasks",                "GET    /api/projects/{projectId}/sprints/{sprintId}/tasks");
        endpoints.put("sprint-task",                 "GET    /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}");
        endpoints.put("create-sprint-task",          "POST   /api/projects/{projectId}/sprints/{sprintId}/tasks");
        endpoints.put("update-sprint-task",          "PATCH  /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}");
        endpoints.put("delete-sprint-task",          "DELETE /api/projects/{projectId}/sprints/{sprintId}/tasks/{sprintTaskId}");
        endpoints.put("sprint-retrospective",        "GET    /api/projects/{projectId}/sprints/{sprintId}/retrospective");
        endpoints.put("create-sprint-retrospective", "POST   /api/projects/{projectId}/sprints/{sprintId}/retrospective");
        endpoints.put("update-sprint-retrospective", "PATCH  /api/projects/{projectId}/sprints/{sprintId}/retrospective");
        endpoints.put("delete-sprint-retrospective", "DELETE /api/projects/{projectId}/sprints/{sprintId}/retrospective");
        endpoints.put("technical-debt",              "GET    /api/projects/{projectId}/technical-debt");
        endpoints.put("technical-debt-item",         "GET    /api/projects/{projectId}/technical-debt/{debtId}");
        endpoints.put("create-technical-debt",       "POST   /api/projects/{projectId}/technical-debt");
        endpoints.put("update-technical-debt",       "PATCH  /api/projects/{projectId}/technical-debt/{debtId}");
        endpoints.put("delete-technical-debt",       "DELETE /api/projects/{projectId}/technical-debt/{debtId}");
        endpoints.put("story-spillovers",            "GET    /api/projects/{projectId}/story-spillovers");
        endpoints.put("story-spillover",             "GET    /api/projects/{projectId}/story-spillovers/{spilloverId}");
        endpoints.put("create-story-spillover",      "POST   /api/projects/{projectId}/story-spillovers");
        endpoints.put("update-story-spillover",      "PATCH  /api/projects/{projectId}/story-spillovers/{spilloverId}");
        endpoints.put("delete-story-spillover",      "DELETE /api/projects/{projectId}/story-spillovers/{spilloverId}");
        endpoints.put("work-items",                  "GET    /api/projects/{projectId}/work-items");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",       "Andromeda Backend API");
        response.put("version",   "0.0.1-SNAPSHOT");
        response.put("status",    "UP");
        response.put("endpoints", endpoints);
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
