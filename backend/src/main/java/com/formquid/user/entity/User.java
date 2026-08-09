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
    }
)
public class User {

    /**
     * Identificador único del usuario.
     *
     * Se genera automáticamente de forma secuencial en la base de datos.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del usuario.
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Correo electrónico usado para iniciar sesión, unico.
     */
    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Contraseña cifrada del usuario.
     *
     * No debe exponerse en respuestas de la API.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

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