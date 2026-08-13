package com.formquid.collaboration.entity;

import com.formquid.survey.entity.Survey;
import com.formquid.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Representa una solicitud pendiente de colaboración en una encuesta.
 *
 * Las solicitudes aceptadas o rechazadas se eliminan, por lo que esta tabla
 * solo almacena solicitudes pendientes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "collaboration_requests",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_collaboration_requests_survey_editor",
            columnNames = {"survey_id", "invited_editor_user_id"}
        )
    },
    indexes = {
        @Index(name = "idx_collaboration_requests_requested_by", columnList = "requested_by_user_id"),
        @Index(name = "idx_collaboration_requests_invited_editor", columnList = "invited_editor_user_id"),
        @Index(name = "idx_collaboration_requests_created_at", columnList = "created_at")
    }
)
public class CollaborationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Encuesta para la que se solicita colaboración.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    /**
     * Usuario que envía la invitación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    /**
     * Editor invitado a colaborar.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_editor_user_id", nullable = false)
    private User invitedEditor;

    /**
     * Mensaje opcional asociado a la solicitud.
     */
    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}