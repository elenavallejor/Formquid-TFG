package com.formquid.registration.entity;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de alta como editor en FormQuid.
 *
 * La cuenta de usuario no se crea hasta que un administrador o
 * superadministrador aprueba la solicitud.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "registration_requests")
public class RegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserGender gender;

    @Column(nullable = false, length = 150)
    private String institution;

    @Column(nullable = false, length = 1000)
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /**
     * Inicializa la fecha de solicitud y el estado inicial antes de guardar.
     */
    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = RegistrationRequestStatus.PENDING;
        }
    }
}