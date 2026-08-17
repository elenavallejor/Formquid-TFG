package com.formquid.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.Locale;

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
@Check(constraints = """
    role in ('SUPERADMIN', 'ADMIN', 'EDITOR')
    and gender in ('FEMALE', 'MALE', 'OTHER')
    and email = lower(btrim(email))
""")
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(
            name = "uk_users_password_setup_token_hash",
            columnNames = "password_setup_token_hash"
        )
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
     * Hash BCrypt de la contraseña del usuario.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Hash SHA-256 del token temporal de configuración de contraseña.
     */
    @Column(name = "password_setup_token_hash", length = 64)
    private String passwordSetupTokenHash;

    /**
     * Fecha límite de uso del token temporal de configuración.
     */
    @Column(name = "password_setup_token_expires_at")
    private LocalDateTime passwordSetupTokenExpiresAt;

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
        normalizeAndValidate();

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Actualiza la fecha de modificación antes de guardar cambios.
     */
    @PreUpdate
    protected void onUpdate() {
        normalizeAndValidate();

        this.updatedAt = LocalDateTime.now();
    }

    private void normalizeAndValidate() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("El correo electrónico del usuario es obligatorio");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalStateException("El hash de contraseña del usuario es obligatorio");
        }

        email = email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Asigna un token temporal de configuración de contraseña.
     */
    public void assignPasswordSetupToken(
        String tokenHash,
        LocalDateTime expiresAt
    ) {
        if (tokenHash == null || tokenHash.isBlank() || expiresAt == null) {
            throw new IllegalArgumentException(
                "El token temporal de contraseña y su expiración son obligatorios"
            );
        }

        this.passwordSetupTokenHash = tokenHash;
        this.passwordSetupTokenExpiresAt = expiresAt;
        this.enabled = false;
    }

    /**
     * Configura una nueva contraseña y activa la cuenta.
     */
    public void completePasswordSetup(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException(
                "El hash de contraseña del usuario es obligatorio"
            );
        }

        this.passwordHash = newPasswordHash;
        this.passwordSetupTokenHash = null;
        this.passwordSetupTokenExpiresAt = null;
        this.enabled = true;
    }

    /**
     * Indica si la cuenta todavía debe configurar su contraseña inicial.
     */
    public boolean hasPendingPasswordSetup() {
        return passwordSetupTokenHash != null
            && !passwordSetupTokenHash.isBlank()
            && passwordSetupTokenExpiresAt != null;
    }
}
