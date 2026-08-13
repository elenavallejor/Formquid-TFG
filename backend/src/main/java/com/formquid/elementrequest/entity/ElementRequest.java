package com.formquid.elementrequest.entity;

import com.formquid.user.entity.User;
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
 * Representa una solicitud realizada por un editor para proponer
 * la creación de un elemento reutilizable del sistema.
 *
 * La propia entidad controla la transición desde PENDING hacia
 * APPROVED o REJECTED para impedir que una solicitud ya resuelta
 * pueda modificarse una segunda vez.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "element_requests",
    indexes = {
        @Index(name = "idx_element_requests_status", columnList = "status"),
        @Index(name = "idx_element_requests_type", columnList = "type"),
        @Index(name = "idx_element_requests_requested_by", columnList = "requested_by_user_id"),
        @Index(name = "idx_element_requests_resolved_by", columnList = "resolved_by_user_id"),
        @Index(name = "idx_element_requests_status_created", columnList = "status, created_at")
    }
)
public class ElementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo de elemento solicitado.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ElementRequestType type;

    /**
     * Título breve de la solicitud.
     */
    @Column(nullable = false, length = 180)
    private String title;

    /**
     * Descripción de la propuesta realizada por el editor.
     */
    @Column(nullable = false, length = 2000)
    private String description;

    /**
     * Justificación opcional de por qué el elemento sería útil.
     */
    @Column(length = 1500)
    private String justification;

    /**
     * Usuario que realiza la solicitud.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    /**
     * Estado actual de la solicitud.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ElementRequestStatus status;

    /**
     * Administrador o superadministrador que resolvió la solicitud.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    /**
     * Comentario opcional asociado a la resolución.
     */
    @Column(name = "resolution_message", length = 1500)
    private String resolutionMessage;

    /**
     * Fecha de creación de la solicitud.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha en la que la solicitud fue resuelta.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Inicializa automáticamente la fecha y el estado de una
     * solicitud nueva antes de persistirla.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ElementRequestStatus.PENDING;
        }
    }

    /**
     * Aprueba una solicitud pendiente.
     *
     * La entidad establece conjuntamente estado, administrador,
     * comentario y fecha de resolución para evitar estados parciales.
     *
     * @param admin usuario que resuelve la solicitud.
     * @param message comentario opcional de resolución.
     */
    public void approve(User admin, String message) {
        resolve(
            ElementRequestStatus.APPROVED,
            admin,
            message
        );
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * @param admin usuario que resuelve la solicitud.
     * @param message comentario opcional de resolución.
     */
    public void reject(User admin, String message) {
        resolve(
            ElementRequestStatus.REJECTED,
            admin,
            message
        );
    }

    /**
     * Centraliza la resolución para garantizar una única transición
     * desde PENDING hacia un estado final.
     */
    private void resolve(
        ElementRequestStatus resolvedStatus,
        User admin,
        String message
    ) {
        validatePending();
        validateResolver(admin);

        this.status = resolvedStatus;
        this.resolvedBy = admin;
        this.resolutionMessage = clean(message);
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Impide volver a resolver una solicitud aprobada o rechazada.
     */
    private void validatePending() {
        if (status != ElementRequestStatus.PENDING) {
            throw new IllegalArgumentException(
                "La solicitud ya ha sido resuelta"
            );
        }
    }

    /**
     * Garantiza que toda resolución conserve la identidad
     * del usuario que la realizó.
     */
    private void validateResolver(User admin) {
        if (admin == null || admin.getId() == null) {
            throw new IllegalArgumentException(
                "El usuario que resuelve la solicitud es obligatorio"
            );
        }
    }

    /**
     * Limpia textos opcionales antes de almacenarlos.
     */
    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
