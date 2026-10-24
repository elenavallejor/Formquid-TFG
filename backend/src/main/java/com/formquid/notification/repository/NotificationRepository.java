package com.formquid.notification.repository;

import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

    /**
     * Lista segura de notificaciones. No carga la entidad User asociada.
     */
    @Query(value = """
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
          and (:unreadOnly = false or notification.readAt is null)
          and (:type is null or notification.type = :type)
        order by case when notification.readAt is null then 0 else 1 end,
            notification.createdAt desc, notification.id desc
    """, countQuery = """
        select count(notification.id)
        from Notification notification
        where notification.recipientUser.id = :userId
          and (:unreadOnly = false or notification.readAt is null)
          and (:type is null or notification.type = :type)
    """)
    Page<NotificationResponseDto> findResponses(
        @Param("userId") Long userId,
        @Param("unreadOnly") boolean unreadOnly,
        @Param("type") NotificationType type,
        Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(
        Long id,
        Long recipientUserId
    );

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt where n.recipientUser.id = :userId and n.readAt is null")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") java.time.Instant readAt);

}
