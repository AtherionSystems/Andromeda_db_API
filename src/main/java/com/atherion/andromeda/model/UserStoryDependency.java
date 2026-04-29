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
@Table(name = "USER_STORY_DEPENDENCIES", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_USD_STORY_ID", columnList = "STORY_ID"),
        @Index(name = "IDX_USD_BLOCKED_BY_ID", columnList = "BLOCKED_BY_ID")
}, uniqueConstraints = {
        @UniqueConstraint(name = "UQ_USD_PAIR", columnNames = {"STORY_ID", "BLOCKED_BY_ID"})
})
public class UserStoryDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "STORY_ID", nullable = false)
    private UserStory story;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "BLOCKED_BY_ID", nullable = false)
    private UserStory blockedBy;

    @NotNull
    @Size(max = 20)
    @ColumnDefault("'blocks'")
    @Column(name = "DEPENDENCY_TYPE", nullable = false, length = 20)
    private String dependencyType;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;
}
