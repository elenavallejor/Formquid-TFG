package com.formquid.registration.entity;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de alta como editor en FormQuid.
 *
 * La cuenta de usuario no se crea hasta que un administrador o
 * superadministrador aprueba la solicitud.
 *
 * La propia entidad controla su resolución para garantizar que una
 * solicitud únicamente pueda pasar una vez desde PENDING a APPROVED
 * o REJECTED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "registration_requests",
    indexes = {
        @Index(name = "idx_registration_requests_email", columnList = "email"),
        @Index(name = "idx_registration_requests_status", columnList = "status"),
        @Index(
            name = "idx_registration_requests_resolved_by",
            columnList = "resolved_by_user_id"
        ),
        @Index(
            name = "idx_registration_requests_status_requested_at",
            columnList = "status, requested_at"
        ),
        @Index(
            name = "idx_registration_requests_status_resolved_at",
            columnList = "status, resolved_at"
        )
    }
)
public class RegistrationRequest {

    /**
     * Identificador único de la solicitud.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del solicitante.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Correo solicitado para la futura cuenta.
     */
    @Column(nullable = false, length = 255)
    private String email;

    /**
     * Contraseña ya cifrada.
     *
     * Nunca se almacena la contraseña original en texto plano.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Género indicado por el solicitante.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserGender gender;

    /**
     * Institución a la que pertenece el solicitante.
     */
    @Column(nullable = false, length = 150)
    private String institution;

    /**
     * Motivación aportada en la solicitud.
     */
    @Column(nullable = false, length = 1000)
    private String motivation;

    /**
     * Estado actual de la solicitud.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RegistrationRequestStatus status;

    /**
     * Fecha en la que se envió la solicitud.
     */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /**
     * Fecha en la que un administrador resolvió la solicitud.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Administrador o superadministrador que resolvió la solicitud.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    /**
     * Motivo de rechazo.
     *
     * Solo tiene contenido cuando la solicitud termina en REJECTED.
     */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /**
     * Inicializa automáticamente la fecha y el estado inicial
     * antes de guardar una nueva solicitud.
     */
    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = RegistrationRequestStatus.PENDING;
        }
    }

    /**
     * Aprueba una solicitud pendiente y registra quién la resolvió.
     *
     * @param admin administrador responsable de la resolución.
     */
    public void approve(User admin) {
        resolve(
            RegistrationRequestStatus.APPROVED,
            admin,
            null
        );
    }

    /**
     * Rechaza una solicitud pendiente y registra el motivo.
     *
     * @param admin administrador responsable de la resolución.
     * @param reason motivo opcional del rechazo.
     */
    public void reject(
        User admin,
        String reason
    ) {
        resolve(
            RegistrationRequestStatus.REJECTED,
            admin,
            cleanOptional(reason)
        );
    }

    /**
     * Centraliza la transición a un estado final.
     *
     * Los campos de auditoría se actualizan conjuntamente para impedir
     * estados parciales, por ejemplo una solicitud resuelta sin resolvedAt
     * o sin resolvedBy.
     */
    private void resolve(
        RegistrationRequestStatus finalStatus,
        User admin,
        String rejectionReason
    ) {
        validatePending();
        validateResolver(admin);

        this.status = finalStatus;
        this.resolvedBy = admin;
        this.resolvedAt = LocalDateTime.now();
        this.rejectionReason = rejectionReason;
    }

    /**
     * Impide modificar una solicitud que ya haya sido resuelta.
     */
    private void validatePending() {
        if (status != RegistrationRequestStatus.PENDING) {
            throw new IllegalArgumentException(
                "La solicitud de registro ya ha sido resuelta"
            );
        }
    }

    /**
     * Garantiza la trazabilidad de toda resolución.
     */
    private void validateResolver(User admin) {
        if (admin == null || admin.getId() == null) {
            throw new IllegalArgumentException(
                "El administrador que resuelve la solicitud es obligatorio"
            );
        }
    }

    /**
     * Limpia textos opcionales antes de almacenarlos.
     */
    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
