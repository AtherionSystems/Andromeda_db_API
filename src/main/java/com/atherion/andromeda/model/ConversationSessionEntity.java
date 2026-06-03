package com.atherion.andromeda.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "CONVERSATION_SESSIONS", schema = "ANDROMEDA_DB")
public class ConversationSessionEntity {

    @Id
    @Column(name = "TELEGRAM_USER_ID", nullable = false)
    private Long telegramUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID",
                referencedColumnName = "ID",
                foreignKey = @ForeignKey(name = "FK_SESSION_USER"),
                nullable = true)
    private User appUser;

    @Column(name = "ACTIVE_PROJECT_ID")
    private Long activeProjectId;

    @Column(name = "ACTIVE_PROJECT_NAME", length = 255)
    private String activeProjectName;

    @Column(name = "ACTIVE_CAP_ID")
    private Long activeCapabilityId;

    @Column(name = "ACTIVE_CAP_NAME", length = 255)
    private String activeCapabilityName;

    @Column(name = "ACTIVE_FEATURE_ID")
    private Long activeFeatureId;

    @Column(name = "ACTIVE_FEATURE_NAME", length = 255)
    private String activeFeatureName;

    @Column(name = "ACTIVE_STORY_ID")
    private Long activeUserStoryId;

    @Column(name = "ACTIVE_STORY_TITLE", length = 500)
    private String activeUserStoryTitle;

    @Column(name = "ACTIVE_TASK_ID")
    private Long activeTaskId;

    @Column(name = "ACTIVE_TASK_TITLE", length = 500)
    private String activeTaskTitle;

    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "HISTORY_JSON")
    private String historyJson;

    @Column(name = "LAST_ACTIVITY")
    private LocalDateTime lastActivity;
}
