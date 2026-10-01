package com.formquid.notification.service;

import com.formquid.collaboration.entity.CollaborationRequest;
import com.formquid.collaboration.entity.PublicationRequest;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.EmailNormalizer;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestiona la creación, consulta, lectura y eliminación de notificaciones.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEmailDispatcher emailDispatcher;

    @Transactional(readOnly = true)
    public PageResponseDto<NotificationResponseDto> findForUser(
        Long userId,
        int page,
        int size,
        boolean unreadOnly,
        NotificationType type
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paginación de notificaciones no válida");
        }

        Page<NotificationResponseDto> result = notificationRepository.findResponses(
            userId, unreadOnly, type, PageRequest.of(page, size)
        );
        return new PageResponseDto<>(result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    public Notification createInternal(User recipient, NotificationType type, String title, String message) {
        return create(recipient, type, title, message, NotificationTargetType.NONE, null);
    }

    public void sendPriorityEmail(String email, NotificationType type, String title, String message) {
        if (!type.requiresEmail()) {
            throw new IllegalArgumentException("El correo requiere un evento de alta prioridad");
        }
        emailDispatcher.send(EmailNormalizer.normalize(email), title, message);
    }

    public Notification createSurveyNotification(User recipient, NotificationType type, String title, String message, Survey survey) {
        return create(recipient, type, title, message, NotificationTargetType.SURVEY, survey.getId());
    }

    public Notification createRegistrationRequestNotification(User recipient, NotificationType type, String title, String message, RegistrationRequest request) {
        return create(recipient, type, title, message, NotificationTargetType.REGISTRATION_REQUEST, request.getId());
    }

    public Notification createCollaborationRequestNotification(User recipient, NotificationType type, String title, String message, CollaborationRequest request) {
        return create(recipient, type, title, message, NotificationTargetType.COLLABORATION_REQUEST, request.getId());
    }

    public Notification createPublicationRequestNotification(User recipient, NotificationType type, String title, String message, PublicationRequest request) {
        return create(recipient, type, title, message, NotificationTargetType.PUBLICATION_REQUEST, request.getId());
    }

    public Notification createElementRequestNotification(User recipient, NotificationType type, String title, String message, ElementRequest request) {
        return create(recipient, type, title, message, NotificationTargetType.ELEMENT_REQUEST, request.getId());
    }

    public Notification createUserNotification(User recipient, NotificationType type, String title, String message, User targetUser) {
        return createUserNotification(recipient, type, title, message, targetUser, recipient.getEmail());
    }

    public Notification createUserNotification(User recipient, NotificationType type, String title, String message,
                                               User targetUser, String emailRecipient) {
        return create(recipient, type, title, message,
            NotificationTargetType.USER, targetUser.getId(), emailRecipient);
    }

    public void notifyAdminsAboutElementRequest(ElementRequest request) {
        adminUserReferences().forEach(user -> createElementRequestNotification(
            user,
            NotificationType.ELEMENT_REQUEST,
            "Nueva solicitud de elemento",
            "Solicitud #" + request.getId() + " de " + request.getRequestedBy().getName() + " para el catálogo (" + request.getElementType() + ").",
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

    public NotificationResponseDto markAsRead(Long id, Long currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        notification.markAsRead();
        return toResponse(notification);
    }

    public void delete(Long id, Long currentUserId) {
        Notification notification = getOwnedNotification(id, currentUserId);
        if (notification.getReadAt() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Lee la notificación antes de eliminarla");
        }
        notificationRepository.delete(notification);
    }

    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, java.time.Instant.now());
    }

    private List<User> adminUserReferences() {
        return userRepository.findIdsByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPERADMIN))
            .stream()
            .map(userRepository::getReferenceById)
            .toList();
    }

    private Notification create(User recipient, NotificationType type, String title, String message,
                                NotificationTargetType targetType, Long targetId) {
        return create(recipient, type, title, message, targetType, targetId, recipient.getEmail());
    }

    private Notification create(User recipient, NotificationType type, String title, String message,
                                NotificationTargetType targetType, Long targetId, String emailRecipient) {
        Notification notification = Notification.builder()
            .recipientUser(recipient)
            .channel(type.requiresEmail() ? NotificationChannel.EMAIL : NotificationChannel.INTERNAL)
            .type(type)
            .title(title)
            .message(message)
            .targetType(targetType)
            .targetId(targetId)
            .build();
        Notification saved = notificationRepository.save(notification);
        if (type.requiresEmail()) {
            emailDispatcher.send(EmailNormalizer.normalize(emailRecipient), title, message);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Notification getOwnedNotification(Long id, Long currentUserId) {
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
