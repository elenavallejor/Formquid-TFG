package com.formquid.notification.entity;

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
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_user_id"),
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_user_id, read_at"),
        @Index(name = "idx_notifications_target", columnList = "target_type, target_id")
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

    /**
     * Tipo del recurso relacionado con la notificación.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private NotificationTargetType targetType;

    /**
     * Identificador del recurso relacionado.
     *
     * Puede ser null cuando la notificación no apunta a ningún recurso.
     */
    @Column(name = "target_id")
    private Long targetId;

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

        if (this.targetType == null) {
            this.targetType = NotificationTargetType.NONE;
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