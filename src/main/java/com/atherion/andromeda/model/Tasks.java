package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TASKS", schema = "ANDROMEDA_DB", indexes = {@Index(name = "IDX_TASKS_PROJECT_ID",
        columnList = "PROJECT_ID")})
public class Tasks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "PROJECT_ID", nullable = false)
    private Project project;

    @Size(max = 255)
    @NotNull
    @Column(name = "TITLE", nullable = false)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Size(max = 10)
    @ColumnDefault("'medium'")
    @Column(name = "PRIORITY", length = 10)
    private String priority;

    @Size(max = 20)
    @ColumnDefault("'todo'")
    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "START_DATE")
    private LocalDateTime startDate;

    @Column(name = "DUE_DATE")
    private LocalDateTime dueDate;

    @Column(name = "ACTUAL_END")
    private LocalDateTime actualEnd;

    @Column(name = "ESTIMATED_HOURS", precision = 4, scale = 1)
    private BigDecimal estimatedHours;

    @Column(name = "ACTUAL_HOURS", precision = 4, scale = 1)
    private BigDecimal actualHours;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "UPDATED_BY")
    private Long updatedBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "USER_STORY_ID")
    private Long userStoryId;


}
