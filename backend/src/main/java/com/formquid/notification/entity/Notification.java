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
import org.hibernate.annotations.Check;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Check(constraints = """
    channel in ('INTERNAL', 'EMAIL')
    and type in (
        'GENERAL', 'REGISTRATION_REQUEST', 'REGISTRATION_APPROVED', 'REGISTRATION_REJECTED',
        'ACCOUNT_UPDATED', 'COLLABORATION_INVITATION', 'COLLABORATION_REQUEST',
        'COLLABORATION_ACCEPTED', 'COLLABORATION_REJECTED', 'PUBLICATION_REQUEST',
        'PUBLICATION_ACCEPTED', 'PUBLICATION_REJECTED', 'ELEMENT_REQUEST',
        'ELEMENT_REQUEST_RESOLVED', 'SURVEY_STATUS_CHANGED', 'SURVEY_OWNERSHIP_TRANSFERRED'
    )
    and target_type in (
        'SURVEY', 'REGISTRATION_REQUEST', 'COLLABORATION_REQUEST', 'PUBLICATION_REQUEST',
        'ELEMENT_REQUEST', 'USER', 'QUESTION', 'BLOCK', 'TEMPLATE', 'NONE'
    )
    and recipient_user_id is not null
""")
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_user_id, created_at"),
        @Index(name = "idx_notifications_recipient_channel_read", columnList = "recipient_user_id, channel, read_at"),
        @Index(name = "idx_notifications_target", columnList = "target_type, target_id")
    }
)
/**
 * Representa una notificación interna o por correo dirigida a un usuario.
 */
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 1500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (targetType == null) {
            targetType = NotificationTargetType.NONE;
        }
    }

    public void markAsRead() {
        if (channel == NotificationChannel.INTERNAL) {
            readAt = Instant.now();
        }
    }
}