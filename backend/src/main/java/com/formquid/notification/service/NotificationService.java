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

/**
 * Gestiona la creación, consulta, lectura y eliminación de notificaciones.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> findForUser(Long userId) {
        return notificationRepository.findResponsesByRecipientUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.INTERNAL);
    }

    public Notification createInternal(User recipient, NotificationType type, String title, String message) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.NONE, null);
    }

    public Notification createEmail(User recipient, NotificationType type, String title, String message) {
        return create(recipient, NotificationChannel.EMAIL, type, title, message, NotificationTargetType.NONE, null);
    }

    public Notification createSurveyNotification(User recipient, NotificationType type, String title, String message, Survey survey) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.SURVEY, survey.getId());
    }

    public Notification createRegistrationRequestNotification(User recipient, NotificationType type, String title, String message, RegistrationRequest request) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.REGISTRATION_REQUEST, request.getId());
    }

    public Notification createCollaborationRequestNotification(User recipient, NotificationType type, String title, String message, CollaborationRequest request) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.COLLABORATION_REQUEST, request.getId());
    }

    public Notification createPublicationRequestNotification(User recipient, NotificationType type, String title, String message, PublicationRequest request) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.PUBLICATION_REQUEST, request.getId());
    }

    public Notification createElementRequestNotification(User recipient, NotificationType type, String title, String message, ElementRequest request) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.ELEMENT_REQUEST, request.getId());
    }

    public Notification createUserNotification(User recipient, NotificationType type, String title, String message, User targetUser) {
        return create(recipient, NotificationChannel.INTERNAL, type, title, message, NotificationTargetType.USER, targetUser.getId());
    }

    public void notifyAdmins(NotificationType type, String title, String message) {
        adminUserReferences().forEach(user -> createInternal(user, type, title, message));
    }

    public void notifyAdminsAboutElementRequest(ElementRequest request) {
        adminUserReferences().forEach(user -> createElementRequestNotification(
            user,
            NotificationType.ELEMENT_REQUEST,
            "Nueva solicitud de elemento",
            "Hay una nueva propuesta de elemento para el catálogo",
            request
        ));
    }

    public void notifyAdminsAboutRegistrationRequest(RegistrationRequest request) {
        adminUserReferences().forEach(user -> createRegistrationRequestNotification(
            user,
            NotificationType.REGISTRATION_REQUEST,
            "Nueva solicitud de registro",
            "Hay una nueva solicitud de cuenta para " + request.getEmail(),
            request
        ));
    }

    @Transactional
    public NotificationResponseDto markAsRead(Long id, Long currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        notification.markAsRead();
        return toResponse(notification);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteRegistrationRequestNotifications(Long requestId) {
        deleteTargetNotifications(NotificationTargetType.REGISTRATION_REQUEST, requestId);
    }

    @Transactional
    public void deleteCollaborationRequestNotifications(Long requestId) {
        deleteTargetNotifications(NotificationTargetType.COLLABORATION_REQUEST, requestId);
    }

    @Transactional
    public void deleteElementRequestNotifications(Long requestId) {
        deleteTargetNotifications(NotificationTargetType.ELEMENT_REQUEST, requestId);
    }

    @Transactional
    public void deletePublicationRequestNotifications(Long requestId) {
        deleteTargetNotifications(NotificationTargetType.PUBLICATION_REQUEST, requestId);
    }

    private void deleteTargetNotifications(NotificationTargetType targetType, Long targetId) {
        notificationRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }

    private List<User> adminUserReferences() {
        return userRepository.findIdsByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPERADMIN))
            .stream()
            .map(userRepository::getReferenceById)
            .toList();
    }

    private Notification create(User recipient, NotificationChannel channel, NotificationType type, String title, String message, NotificationTargetType targetType, Long targetId) {
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

    private Notification getOwnedNotification(Long id, Long currentUserId) {
        return notificationRepository.findByIdAndRecipientUserId(id, currentUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notificación no encontrada"));
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