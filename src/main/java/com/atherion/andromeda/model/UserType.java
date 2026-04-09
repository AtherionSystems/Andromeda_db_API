package com.atherion.andromeda.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "USER_TYPE", schema = "ANDROMEDA_DB", uniqueConstraints = {@UniqueConstraint(name = "SYS_C0030179",
        columnNames = {"USER_TYPE"})})
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Column(name = "USER_TYPE", nullable = false, length = 50)
    private String userType;

    @Size(max = 500)
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "CREATED_AT")
    private Instant createdAt;


}