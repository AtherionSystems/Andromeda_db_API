package com.atherion.andromeda.model;

import jakarta.persistence.*;
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
@Table(name = "LOGS", schema = "ANDROMEDA_DB", indexes = {
        @Index(name = "IDX_LOGS_ENTITY", columnList = "ENTITY,ENTITY_ID")
})
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Size(max = 50)
    @Column(name = "ENTITY", length = 50)
    private String entity;

    @Column(name = "ENTITY_ID")
    private Long entityId;

    @Size(max = 50)
    @Column(name = "ACTION", length = 50)
    private String action;

    @Size(max = 1000)
    @Column(name = "DETAIL", length = 1000)
    private String detail;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "LOG_DATE")
    private LocalDateTime logDate;
}
