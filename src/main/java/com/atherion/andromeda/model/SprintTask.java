package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "SPRINT_TASKS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_ST_SPRINT_ID",
                columnList = "SPRINT_ID"),
        @Index(name = "IDX_ST_TASK_ID",
                columnList = "TASK_ID")})
public class SprintTask {
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "TASK_ID", nullable = false)
    private Tasks task;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ADDED_AT")
    private Instant addedAt;

    @Column(name = "REMOVED_AT")
    private Instant removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "MOVED_TO")
    private Sprint movedTo;


}