package com.formquid.notification.service;

import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.common.exception.ApiException;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.repository.NotificationRepository;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.survey.entity.Survey;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<NotificationResponseDto> findForUser(UUID userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.INTERNAL);
    }

    /**
     * Recibe usuario destino y mensaje. Devuelve la notificación interna creada.
     */
    public Notification createInternal(
        User recipient,
        NotificationType type,
        String title,
        String message
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.NONE, null);
    }

    public Notification createEmail(
        User recipient,
        NotificationType type,
        String title,
        String message
    ) {
        // Registra el email pendiente; el correo real se obtiene desde recipient.email.
        return create(recipient, NotificationChannel.EMAIL, type, title, message, NotificationTargetType.NONE, null);
    }

    public Notification createSurveyNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        Survey survey
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.SURVEY, survey.getId());
    }

    public Notification createRegistrationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        RegistrationRequest request
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.REGISTRATION_REQUEST, request.getId().toString());
    }

    public Notification createRegistrationApprovedEmail(
        User recipient,
        RegistrationRequest request
    ) {
        return create(
            recipient,
            NotificationChannel.EMAIL,
            NotificationType.REGISTRATION_APPROVED,
            "Solicitud aprobada",
            "Tu cuenta de FormQuid ha sido aprobada.",
            NotificationTargetType.REGISTRATION_REQUEST,
            request.getId().toString()
        );
    }

    public Notification createCollaborationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        CollaborationRequest request
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.COLLABORATION_REQUEST, request.getId().toString());
    }

    public Notification createPublicationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        PublicationRequest request
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.PUBLICATION_REQUEST, request.getId().toString());
    }

    public Notification createElementRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        ElementRequest request
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.ELEMENT_REQUEST, request.getId().toString());
    }

    public Notification createUserNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        User targetUser
    ) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.USER, targetUser.getId().toString());
    }

    public void notifyAdmins(NotificationType type, String title, String message) {
        userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPERADMIN)
            .forEach(user -> createInternal(user, type, title, message));
    }

    public void notifyAdminsAboutElementRequest(ElementRequest request) {
        userRepository.findAll().stream()
            .filter(user -> user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPERADMIN)
            .forEach(user -> createElementRequestNotification(
                user,
                NotificationType.ELEMENT_REQUEST,
                "Nueva solicitud de elemento",
                "Hay una nueva propuesta de elemento para el catálogo",
                request
            ));
    }

    /**
     * Recibe una notificación interna. Devuelve la notificación leída.
     */
    @Transactional
    public NotificationResponseDto markAsRead(Long id, UUID currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        notification.markAsRead();
        return toResponse(notification);
    }

    /**
     * Recibe una notificación y el usuario actual. Elimina la notificación si le pertenece.
     */
    @Transactional
    public void delete(Long id, UUID currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        notificationRepository.delete(notification);
    }

    private Notification create(
        User recipient,
        NotificationChannel channel,
        NotificationType type,
        String title,
        String message,
        NotificationTargetType targetType,
        String targetId
    ) {
        Notification notification = Notification.builder()
            .recipientUser(recipient)
            .channel(channel)
            .type(type)
            .title(title)
            .message(message)
            .targetType(targetType)
            .targetId(targetId)
            .build();
        return notificationRepository.save(notification);
    }

    private Notification getOwnedNotification(Long id, UUID currentUserId) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
        if (!notification.getRecipientUser().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "La notificación no pertenece al usuario actual");
        }
        return notification;
    }

    private NotificationResponseDto toResponse(Notification notification) {
        return new NotificationResponseDto(
            notification.getId(),
            notification.getType(),
            notification.getChannel(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getTargetType(),
            notification.getTargetId(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}