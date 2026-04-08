package com.atherion.andromeda.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rol_id", referencedColumnName = "id")
    private Rol rol;

    @Column(name = "name")
    private String name;

    @Column(name = "username", length = 20, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 20)
    private String passwordHash;
}