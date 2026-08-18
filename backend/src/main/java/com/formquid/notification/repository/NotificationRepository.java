package com.formquid.notification.repository;

import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    long countByRecipientUserIdAndChannelAndReadAtIsNull(UUID recipientUserId, NotificationChannel channel);
}
