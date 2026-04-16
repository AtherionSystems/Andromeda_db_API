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
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SPRINTS", schema = "ANDROMEDA_DB", indexes = {@Index(name = "IDX_SPRINTS_PROJECT_ID",
        columnList = "PROJECT_ID")})
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Size(max = 100)
    @NotNull
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Size(max = 1000)
    @Column(name = "GOAL", length = 1000)
    private String goal;

    @Size(max = 20)
    @ColumnDefault("'planned'")
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "START_DATE")
    private LocalDateTime startDate;

    @Column(name = "DUE_DATE")
    private LocalDateTime dueDate;

    @Column(name = "ACTUAL_END")
    private LocalDateTime actualEnd;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;


}