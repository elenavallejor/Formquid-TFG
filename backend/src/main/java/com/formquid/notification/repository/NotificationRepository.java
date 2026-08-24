package com.formquid.notification.repository;

import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

    /**
     * Lista segura de notificaciones. No carga la entidad User asociada.
     */
    @Query("""
        select new com.formquid.notification.dto.NotificationResponseDto(
            notification.id,
            notification.type,
            notification.channel,
            notification.title,
            notification.message,
            notification.targetType,
            notification.targetId,
            notification.createdAt,
            notification.readAt
        )
        from Notification notification
        where notification.recipientUser.id = :userId
        order by notification.createdAt desc
    """)
    List<NotificationResponseDto> findResponsesByRecipientUserId(
        @Param("userId") UUID userId
    );

    long countByRecipientUserIdAndChannelAndReadAtIsNull(
        UUID recipientUserId,
        NotificationChannel channel
    );

    Optional<Notification> findByIdAndRecipientUserId(
        Long id,
        UUID recipientUserId
    );

    void deleteByTargetTypeAndTargetId(
        NotificationTargetType targetType,
        String targetId
    );
}
