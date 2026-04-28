package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TECHNICAL_DEBT", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_DEBT_PROJECT_ID", columnList = "PROJECT_ID"),
        @Index(name = "IDX_DEBT_STORY_ID", columnList = "USER_STORY_ID"),
        @Index(name = "IDX_DEBT_STATUS", columnList = "STATUS")
})
public class TechnicalDebt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "USER_STORY_ID")
    private UserStory userStory;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "TASK_ID")
    private Tasks task;

    @NotNull
    @Size(max = 255)
    @Column(name = "TITLE", nullable = false)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @NotNull
    @Size(max = 30)
    @Column(name = "DEBT_TYPE", nullable = false, length = 30)
    private String debtType;

    @Size(max = 10)
    @ColumnDefault("'medium'")
    @Column(name = "PRIORITY", length = 10)
    private String priority;

    @Size(max = 20)
    @ColumnDefault("'open'")
    @Column(name = "STATUS", length = 20)
    private String status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "ASSIGNED_TO", nullable = false)
    private User assignedTo;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private User createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "UPDATED_BY")
    private User updatedBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
