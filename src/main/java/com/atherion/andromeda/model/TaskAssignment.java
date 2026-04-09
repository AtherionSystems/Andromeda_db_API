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
@Table(name = "TASK_ASSIGNMENTS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_TA_TASK_ID",
                columnList = "TASK_ID"),
        @Index(name = "IDX_TA_USER_ID",
                columnList = "USER_ID")}, uniqueConstraints = {@UniqueConstraint(name = "UQ_TA_TASK_USER",
        columnNames = {
                "TASK_ID",
                "USER_ID"})})
public class TaskAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "TASK_ID", nullable = false)
    private Tasks task;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ASSIGNED_AT")
    private Instant assignedAt;


}