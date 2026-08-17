package com.formquid.notification.entity;

import com.formquid.collaboration.entity.SurveyCollaboration;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.survey.entity.Survey;
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

import java.time.LocalDateTime;

/**
 * Representa una notificación dirigida a un usuario del sistema.
 *
 * Puede ser una notificación interna o el registro de una notificación
 * enviada por correo electrónico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    type in (
        'GENERAL',
        'REGISTRATION_APPROVED',
        'ACCOUNT_UPDATED',
        'COLLABORATION_REQUEST',
        'COLLABORATION_ACCEPTED',
        'COLLABORATION_REJECTED',
        'ELEMENT_REQUEST_RESOLVED',
        'SURVEY_STATUS_CHANGED'
    )
    and channel in ('INTERNAL', 'EMAIL')
    and (
        (case when survey_id is null then 0 else 1 end)
        + (case when registration_request_id is null then 0 else 1 end)
        + (case when collaboration_id is null then 0 else 1 end)
        + (case when element_request_id is null then 0 else 1 end)
        + (case when target_user_id is null then 0 else 1 end)
    ) <= 1
""")
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_user_id"),
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_user_id, read_at"),
        @Index(name = "idx_notifications_survey", columnList = "survey_id"),
        @Index(name = "idx_notifications_registration_request", columnList = "registration_request_id"),
        @Index(name = "idx_notifications_collaboration", columnList = "collaboration_id"),
        @Index(name = "idx_notifications_element_request", columnList = "element_request_id"),
        @Index(name = "idx_notifications_target_user", columnList = "target_user_id")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario destinatario de la notificación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    /**
     * Tipo funcional de la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Canal de envío o registro de la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    /**
     * Título breve visible para el usuario.
     */
    @Column(nullable = false, length = 180)
    private String title;

    /**
     * Mensaje completo de la notificación.
     */
    @Column(nullable = false, length = 1500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id")
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_request_id")
    private RegistrationRequest registrationRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboration_id")
    private SurveyCollaboration collaboration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_request_id")
    private ElementRequest elementRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    /**
     * Fecha de creación de la notificación.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha de lectura.
     *
     * Si es null, la notificación sigue sin leerse.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.channel == null) {
            this.channel = NotificationChannel.INTERNAL;
        }
    }

    /**
     * Marca la notificación como leída.
     */
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }
}
