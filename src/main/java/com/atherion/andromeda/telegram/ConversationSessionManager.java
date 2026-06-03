package com.atherion.andromeda.telegram;

import com.atherion.andromeda.model.ConversationSessionEntity;
import com.atherion.andromeda.model.User;
import com.atherion.andromeda.repositories.ConversationSessionRepository;
import com.atherion.andromeda.repositories.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationSessionManager {

    private final ConversationSessionRepository sessionRepo;
    private final UserRepository                userRepo;

    private final ObjectMapper                  objectMapper = new ObjectMapper();
    private final Map<Long, ConversationSession> cache = new ConcurrentHashMap<>();

    // ── read ──────────────────────────────────────────────────────────────────

    public ConversationSession getOrCreate(Long telegramUserId) {
        return cache.computeIfAbsent(telegramUserId, this::loadOrCreate);
    }

    // ── write-through setters ─────────────────────────────────────────────────

    @Transactional
    public void setActiveProject(Long telegramUserId, Long projectId, String projectName) {
        if (telegramUserId == null) return;
        ConversationSession session = getOrCreate(telegramUserId);
        session.setActiveProject(projectId, projectName);
        persist(telegramUserId, session);
    }

    @Transactional
    public void setActiveCapability(Long telegramUserId, Long capabilityId, String capabilityName) {
        if (telegramUserId == null) return;
        ConversationSession session = getOrCreate(telegramUserId);
        session.setActiveCapability(capabilityId, capabilityName);
        persist(telegramUserId, session);
    }

    @Transactional
    public void setActiveFeature(Long telegramUserId, Long featureId, String featureName) {
        if (telegramUserId == null) return;
        ConversationSession session = getOrCreate(telegramUserId);
        session.setActiveFeature(featureId, featureName);
        persist(telegramUserId, session);
    }

    @Transactional
    public void setActiveUserStory(Long telegramUserId, Long userStoryId, String userStoryTitle) {
        if (telegramUserId == null) return;
        ConversationSession session = getOrCreate(telegramUserId);
        session.setActiveUserStory(userStoryId, userStoryTitle);
        persist(telegramUserId, session);
    }

    @Transactional
    public void setActiveTask(Long telegramUserId, Long taskId, String taskTitle) {
        if (telegramUserId == null) return;
        ConversationSession session = getOrCreate(telegramUserId);
        session.setActiveTask(taskId, taskTitle);
        persist(telegramUserId, session);
    }

    /** Called by AiIntentRouter after each AI exchange to persist updated history. */
    @Transactional
    public void persistHistory(Long telegramUserId) {
        if (telegramUserId == null) return;
        ConversationSession session = cache.get(telegramUserId);
        if (session != null) persist(telegramUserId, session);
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private ConversationSession loadOrCreate(Long telegramUserId) {
        return sessionRepo.findById(telegramUserId)
                .map(this::toSession)
                .orElseGet(() -> new ConversationSession(telegramUserId));
    }

    private ConversationSession toSession(ConversationSessionEntity e) {
        ConversationSession s = new ConversationSession(e.getTelegramUserId());
        if (e.getActiveProjectId() != null)
            s.setActiveProject(e.getActiveProjectId(), e.getActiveProjectName());
        if (e.getActiveCapabilityId() != null)
            s.setActiveCapability(e.getActiveCapabilityId(), e.getActiveCapabilityName());
        if (e.getActiveFeatureId() != null)
            s.setActiveFeature(e.getActiveFeatureId(), e.getActiveFeatureName());
        if (e.getActiveUserStoryId() != null)
            s.setActiveUserStory(e.getActiveUserStoryId(), e.getActiveUserStoryTitle());
        if (e.getActiveTaskId() != null)
            s.setActiveTask(e.getActiveTaskId(), e.getActiveTaskTitle());
        restoreHistory(s, e.getHistoryJson());
        return s;
    }

    private void restoreHistory(ConversationSession session, String historyJson) {
        if (historyJson == null || historyJson.isBlank()) return;
        try {
            List<Map<String, String>> turns = objectMapper.readValue(
                    historyJson, new TypeReference<>() {});
            turns.forEach(t -> session.addToHistory(t.get("role"), t.get("content")));
        } catch (Exception ex) {
            log.warn("Could not restore conversation history: {}", ex.getMessage());
        }
    }

    private void persist(Long telegramUserId, ConversationSession session) {
        try {
            ConversationSessionEntity entity = sessionRepo.findById(telegramUserId)
                    .orElseGet(() -> {
                        ConversationSessionEntity e = new ConversationSessionEntity();
                        e.setTelegramUserId(telegramUserId);
                        return e;
                    });

            // Link to app user if not already linked
            if (entity.getAppUser() == null) {
                userRepo.findByTelegramId(telegramUserId)
                        .ifPresent(entity::setAppUser);
            }

            entity.setActiveProjectId(session.getActiveProjectId());
            entity.setActiveProjectName(session.getActiveProjectName());
            entity.setActiveCapabilityId(session.getActiveCapabilityId());
            entity.setActiveCapabilityName(session.getActiveCapabilityName());
            entity.setActiveFeatureId(session.getActiveFeatureId());
            entity.setActiveFeatureName(session.getActiveFeatureName());
            entity.setActiveUserStoryId(session.getActiveUserStoryId());
            entity.setActiveUserStoryTitle(session.getActiveUserStoryTitle());
            entity.setActiveTaskId(session.getActiveTaskId());
            entity.setActiveTaskTitle(session.getActiveTaskTitle());
            entity.setHistoryJson(serializeHistory(session.getHistory()));
            entity.setLastActivity(
                    LocalDateTime.ofInstant(session.getLastActivity(), ZoneOffset.UTC));

            sessionRepo.save(entity);
        } catch (Exception ex) {
            log.error("Failed to persist conversation session for user {}: {}", telegramUserId, ex.getMessage());
        }
    }

    private String serializeHistory(List<ChatMessage> history) {
        if (history.isEmpty()) return null;
        try {
            List<Map<String, String>> turns = history.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                    .toList();
            return objectMapper.writeValueAsString(turns);
        } catch (Exception ex) {
            log.warn("Could not serialize conversation history: {}", ex.getMessage());
            return null;
        }
    }
}
