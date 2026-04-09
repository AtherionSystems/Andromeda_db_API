package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "PROJECT_MEMBERS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_PM_PROJECT_ID",
                columnList = "PROJECT_ID"),
        @Index(name = "IDX_PM_USER_ID",
                columnList = "USER_ID")}, uniqueConstraints = {@UniqueConstraint(name = "UQ_PM_PROJECT_USER",
        columnNames = {
                "PROJECT_ID",
                "USER_ID"})})
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Size(max = 20)
    @ColumnDefault("'member'")
    @Column(name = "ROLE", length = 20)
    private String role;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "JOINED_AT")
    private Instant joinedAt;


}