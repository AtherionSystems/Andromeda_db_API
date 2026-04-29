package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SPRINT_STORIES_ASSIGNMENTS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_SS_SPRINT_ID", columnList = "SPRINT_ID"),
        @Index(name = "IDX_SS_STORY_ID", columnList = "USER_STORY_ID")
})
public class SprintStoryAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "SPRINT_ID", nullable = false)
    private Sprint sprint;

    @NotNull
    @Column(name = "USER_STORY_ID", nullable = false)
    private Long userStoryId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ADDED_AT")
    private LocalDateTime addedAt;

    @Column(name = "REMOVED_AT")
    private LocalDateTime removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "MOVED_TO")
    private Sprint movedTo;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive;
}
