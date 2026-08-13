package com.formquid.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa a un usuario registrado en FormQuid.
 *
 * Almacena los datos necesarios para autenticación, autorización
 * y gestión básica del perfil.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_enabled", columnList = "enabled"),
        @Index(name = "idx_users_role_enabled", columnList = "role, enabled")
    }
)
public class User {

    /**
     * Identificador único del usuario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del usuario.
     */
    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Correo electrónico usado para iniciar sesión.
     */
    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Contraseña cifrada del usuario.
     *
     * Aunque el atributo se llame password, nunca se almacena en texto plano.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Rol del usuario dentro del sistema.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * Género indicado por el usuario.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserGender gender;

    /**
     * Institución o entidad asociada al usuario.
     */
    @Column(length = 150)
    private String institution;

    /**
     * Indica si la cuenta está activa.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Fecha de creación del usuario.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha de última modificación del usuario.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Asigna las fechas iniciales antes de guardar el usuario.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Actualiza la fecha de modificación antes de guardar cambios.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}