package com.formquid.elementrequest.entity;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Solicitud realizada por un editor para proponer un elemento reutilizable
 * del catálogo.
 *
 * Las solicitudes se conservan después de resolverse para mantener auditoría
 * y métricas de uso de la plataforma.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    type in ('BLOCK', 'QUESTION', 'TEMPLATE')
    and status in ('PENDING', 'APPROVED', 'REJECTED')
    and jsonb_typeof(request_payload) = 'object'
    and (
        (
            status = 'PENDING'
            and resolved_at is null
            and resolved_by_user_id is null
        )
        or (
            status in ('APPROVED', 'REJECTED')
            and resolved_at is not null
            and resolved_by_user_id is not null
        )
    )
""")
@Table(
    name = "element_requests",
    indexes = {
        @Index(name = "idx_element_requests_type", columnList = "type"),
        @Index(name = "idx_element_requests_status", columnList = "status"),
        @Index(name = "idx_element_requests_requested_by", columnList = "requested_by_user_id"),
        @Index(name = "idx_element_requests_resolved_by", columnList = "resolved_by_user_id"),
        @Index(name = "idx_element_requests_created_at", columnList = "created_at")
    }
)
public class ElementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de elemento que se propone crear. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ElementRequestType type;

    /** Título breve utilizado para identificar la propuesta. */
    @Column(nullable = false, length = 180)
    private String title;

    /** Descripción de la propuesta. */
    @Column(nullable = false, length = 2000)
    private String description;

    /** Justificación opcional proporcionada por el editor. */
    @Column(length = 1500)
    private String justification;

    /**
     * Definición completa del elemento propuesto.
     *
     * Se almacena como JSONB porque la estructura depende del tipo.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode requestPayload;

    /** Usuario que realizó la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    /** Estado de resolución de la solicitud. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ElementRequestStatus status;

    /** Fecha en la que se creó la solicitud. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Fecha de resolución administrativa. */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Usuario administrador que resolvió la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    /** Mensaje opcional de resolución. */
    @Column(name = "resolution_message", length = 1500)
    private String resolutionMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = ElementRequestStatus.PENDING;
        }
    }

    public void approve(User admin, String message) {
        resolve(ElementRequestStatus.APPROVED, admin, message);
    }

    public void reject(User admin, String message) {
        resolve(ElementRequestStatus.REJECTED, admin, message);
    }

    private void resolve(
        ElementRequestStatus finalStatus,
        User admin,
        String message
    ) {
        if (status != ElementRequestStatus.PENDING) {
            throw new IllegalStateException(
                "La solicitud de elemento ya ha sido resuelta"
            );
        }

        if (admin == null || admin.getId() == null) {
            throw new IllegalStateException(
                "El administrador que resuelve la solicitud es obligatorio"
            );
        }

        this.status = finalStatus;
        this.resolvedBy = admin;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionMessage =
            message != null && !message.isBlank() ? message.trim() : null;
    }
}
