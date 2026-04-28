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
@Table(name = "STORY_SPILLOVERS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_SPILL_STORY_ID", columnList = "USER_STORY_ID"),
        @Index(name = "IDX_SPILL_ORIGIN", columnList = "ORIGIN_SPRINT_ID")
})
public class StorySpillover {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "SPRINT_STORY_ID", nullable = false)
    private SprintStoryAssignment sprintStory;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "USER_STORY_ID", nullable = false)
    private UserStory userStory;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "ORIGIN_SPRINT_ID", nullable = false)
    private Sprint originSprint;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "DESTINATION_SPRINT_ID", nullable = false)
    private Sprint destinationSprint;

    @NotNull
    @Size(max = 50)
    @Column(name = "REASON", nullable = false, length = 50)
    private String reason;

    @Size(max = 1000)
    @Column(name = "DETAIL", length = 1000)
    private String detail;

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
