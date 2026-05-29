package com.atherion.andromeda.telegram;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ConversationSession {

    private static final int MAX_HISTORY = 10; // 5 exchanges

    private final Long userId;
    private final Deque<ChatMessage> history = new ArrayDeque<>();

    private Long   activeProjectId;
    private String activeProjectName;

    private Long   activeCapabilityId;
    private String activeCapabilityName;

    private Long   activeFeatureId;
    private String activeFeatureName;

    private Long   activeUserStoryId;
    private String activeUserStoryTitle;

    private Long   activeTaskId;
    private String activeTaskTitle;

    private Instant lastActivity = Instant.now();

    public ConversationSession(Long userId) {
        this.userId = userId;
    }

    // ── setters ───────────────────────────────────────────────────────────────

    public void setActiveProject(Long id, String name) {
        activeProjectId      = id;
        activeProjectName    = name;
        activeCapabilityId   = null;
        activeCapabilityName = null;
        activeFeatureId      = null;
        activeFeatureName    = null;
        activeUserStoryId    = null;
        activeUserStoryTitle = null;
        activeTaskId         = null;
        activeTaskTitle      = null;
        touch();
    }

    public void setActiveCapability(Long id, String name) {
        activeCapabilityId   = id;
        activeCapabilityName = name;
        activeFeatureId      = null;
        activeFeatureName    = null;
        activeUserStoryId    = null;
        activeUserStoryTitle = null;
        touch();
    }

    public void setActiveFeature(Long id, String name) {
        activeFeatureId      = id;
        activeFeatureName    = name;
        activeUserStoryId    = null;
        activeUserStoryTitle = null;
        touch();
    }

    public void setActiveUserStory(Long id, String title) {
        activeUserStoryId    = id;
        activeUserStoryTitle = title;
        touch();
    }

    public void setActiveTask(Long id, String title) {
        activeTaskId    = id;
        activeTaskTitle = title;
        touch();
    }

    // ── getters ───────────────────────────────────────────────────────────────

    public Long   getActiveProjectId()       { return activeProjectId; }
    public String getActiveProjectName()     { return activeProjectName; }
    public Long   getActiveCapabilityId()    { return activeCapabilityId; }
    public String getActiveCapabilityName()  { return activeCapabilityName; }
    public Long   getActiveFeatureId()       { return activeFeatureId; }
    public String getActiveFeatureName()     { return activeFeatureName; }
    public Long   getActiveUserStoryId()     { return activeUserStoryId; }
    public String getActiveUserStoryTitle()  { return activeUserStoryTitle; }
    public Long   getActiveTaskId()          { return activeTaskId; }
    public String getActiveTaskTitle()       { return activeTaskTitle; }
    public Instant getLastActivity()         { return lastActivity; }

    // ── conversation history ──────────────────────────────────────────────────

    public void addToHistory(String role, String content) {
        history.addLast(new ChatMessage(role, content));
        while (history.size() > MAX_HISTORY) history.removeFirst();
    }

    public List<ChatMessage> getHistory() {
        return List.copyOf(history);
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public boolean hasActiveProject()    { return activeProjectId    != null; }
    public boolean hasActiveCapability() { return activeCapabilityId != null; }
    public boolean hasActiveFeature()    { return activeFeatureId    != null; }
    public boolean hasActiveUserStory()  { return activeUserStoryId  != null; }
    public boolean hasActiveTask()       { return activeTaskId       != null; }

    // ── AI prompt support ─────────────────────────────────────────────────────

    /** Returns a compact context block for injection into AI system prompts. */
    public String buildContextSummary() {
        StringBuilder sb = new StringBuilder();
        if (activeProjectId != null)
            sb.append("Active project: \"").append(activeProjectName)
              .append("\" (ID ").append(activeProjectId).append(")\n");
        if (activeCapabilityId != null)
            sb.append("Active capability: \"").append(activeCapabilityName)
              .append("\" (ID ").append(activeCapabilityId).append(")\n");
        if (activeFeatureId != null)
            sb.append("Active feature: \"").append(activeFeatureName)
              .append("\" (ID ").append(activeFeatureId).append(")\n");
        if (activeUserStoryId != null)
            sb.append("Active user story: \"").append(activeUserStoryTitle)
              .append("\" (ID ").append(activeUserStoryId).append(")\n");
        if (activeTaskId != null)
            sb.append("Active task: \"").append(activeTaskTitle)
              .append("\" (ID ").append(activeTaskId).append(")\n");
        return sb.toString().trim();
    }

    private void touch() {
        lastActivity = Instant.now();
    }
}
