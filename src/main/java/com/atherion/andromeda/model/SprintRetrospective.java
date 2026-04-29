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
@Table(name = "SPRINT_RETROSPECTIVES", schema = "ANDROMEDA_DB", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_RETRO_SPRINT_ID", columnNames = "SPRINT_ID")
})
public class SprintRetrospective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "SPRINT_ID", nullable = false)
    private Sprint sprint;

    @Lob
    @Column(name = "SUMMARY")
    private String summary;

    @Lob
    @Column(name = "WHAT_WENT_WELL")
    private String whatWentWell;

    @Lob
    @Column(name = "WHAT_WENT_WRONG")
    private String whatWentWrong;

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
