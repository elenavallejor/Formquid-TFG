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

    /**
     * Devuelve todas las notificaciones de un usuario.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> findForUser(UUID userId) {
        return notificationRepository
            .findByRecipientUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Devuelve el número de notificaciones internas no leídas.
     */
    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository
            .countByRecipientUserIdAndChannelAndReadAtIsNull(
                userId,
                NotificationChannel.INTERNAL
            );
    }

    /**
     * Crea una notificación interna sin recurso asociado.
     */
    public Notification createInternal(
        User recipient,
        NotificationType type,
        String title,
        String message
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.NONE,
            null
        );
    }

    /**
     * Registra una notificación de correo sin recurso asociado.
     */
    public Notification createEmail(
        User recipient,
        NotificationType type,
        String title,
        String message
    ) {
        return create(
            recipient,
            NotificationChannel.EMAIL,
            type,
            title,
            message,
            NotificationTargetType.NONE,
            null
        );
    }

    /**
     * Crea una notificación asociada a una encuesta.
     */
    public Notification createSurveyNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        Survey survey
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.SURVEY,
            survey.getId().toString()
        );
    }

    /**
     * Crea una notificación asociada a una solicitud de registro.
     */
    public Notification createRegistrationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        RegistrationRequest request
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.REGISTRATION_REQUEST,
            request.getId().toString()
        );
    }

    /**
     * Crea una notificación asociada a una solicitud de colaboración.
     */
    public Notification createCollaborationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        CollaborationRequest request
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.COLLABORATION_REQUEST,
            request.getId().toString()
        );
    }

    /**
     * Crea una notificación asociada a una solicitud de publicación.
     */
    public Notification createPublicationRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        PublicationRequest request
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.PUBLICATION_REQUEST,
            request.getId().toString()
        );
    }

    /**
     * Crea una notificación asociada a una solicitud de elemento.
     */
    public Notification createElementRequestNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        ElementRequest request
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.ELEMENT_REQUEST,
            request.getId().toString()
        );
    }

    /**
     * Crea una notificación asociada a un usuario.
     */
    public Notification createUserNotification(
        User recipient,
        NotificationType type,
        String title,
        String message,
        User targetUser
    ) {
        return create(
            recipient,
            NotificationChannel.INTERNAL,
            type,
            title,
            message,
            NotificationTargetType.USER,
            targetUser.getId().toString()
        );
    }

    /**
     * Notifica a todos los administradores.
     */
    public void notifyAdmins(
        NotificationType type,
        String title,
        String message
    ) {
        userRepository.findAll()
            .stream()
            .filter(user ->
                user.getRole() == UserRole.ADMIN
                    || user.getRole() == UserRole.SUPERADMIN
            )
            .forEach(user ->
                createInternal(
                    user,
                    type,
                    title,
                    message
                )
            );
    }

    /**
     * Notifica a los administradores sobre una solicitud de elemento.
     */
    public void notifyAdminsAboutElementRequest(
        ElementRequest request
    ) {
        userRepository.findAll()
            .stream()
            .filter(user ->
                user.getRole() == UserRole.ADMIN
                    || user.getRole() == UserRole.SUPERADMIN
            )
            .forEach(user ->
                createElementRequestNotification(
                    user,
                    NotificationType.ELEMENT_REQUEST,
                    "Nueva solicitud de elemento",
                    "Hay una nueva propuesta de elemento para el catálogo",
                    request
                )
            );
    }

    /**
     * Marca una notificación interna como leída.
     */
    @Transactional
    public NotificationResponseDto markAsRead(
        Long id,
        UUID currentUserId
    ) {
        Notification notification =
            getOwnedNotification(id, currentUserId);

        notification.markAsRead();

        return toResponse(notification);
    }

    /**
     * Elimina una notificación del usuario actual.
     */
    @Transactional
    public void delete(
        Long id,
        UUID currentUserId
    ) {
        Notification notification =
            getOwnedNotification(id, currentUserId);

        notificationRepository.delete(notification);
    }

    /**
     * Elimina las notificaciones asociadas a una solicitud de registro.
     */
    @Transactional
    public void deleteRegistrationRequestNotifications(
        UUID requestId
    ) {
        deleteTargetNotifications(
            NotificationTargetType.REGISTRATION_REQUEST,
            requestId
        );
    }

    /**
     * Elimina las notificaciones asociadas a una solicitud de colaboración.
     */
    @Transactional
    public void deleteCollaborationRequestNotifications(
        UUID requestId
    ) {
        deleteTargetNotifications(
            NotificationTargetType.COLLABORATION_REQUEST,
            requestId
        );
    }

    /**
     * Elimina las notificaciones asociadas a una solicitud de elemento.
     */
    @Transactional
    public void deleteElementRequestNotifications(
        UUID requestId
    ) {
        deleteTargetNotifications(
            NotificationTargetType.ELEMENT_REQUEST,
            requestId
        );
    }

    /**
     * Elimina las notificaciones asociadas a una solicitud de publicación.
     */
    @Transactional
    public void deletePublicationRequestNotifications(
        UUID requestId
    ) {
        deleteTargetNotifications(
            NotificationTargetType.PUBLICATION_REQUEST,
            requestId
        );
    }

    /**
     * Elimina las notificaciones asociadas a un recurso.
     */
    private void deleteTargetNotifications(
        NotificationTargetType targetType,
        UUID targetId
    ) {
        notificationRepository.deleteByTargetTypeAndTargetId(
            targetType,
            targetId.toString()
        );
    }

    /**
     * Crea y persiste una notificación.
     */
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

    /**
     * Obtiene una notificación perteneciente al usuario actual.
     */
    private Notification getOwnedNotification(
        Long id,
        UUID currentUserId
    ) {
        Notification notification =
            notificationRepository.findById(id)
                .orElseThrow(() ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Notificación no encontrada"
                    )
                );

        if (
            !notification
                .getRecipientUser()
                .getId()
                .equals(currentUserId)
        ) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "La notificación no pertenece al usuario actual"
            );
        }

        return notification;
    }

    /**
     * Convierte una notificación en su DTO de respuesta.
     */
    private NotificationResponseDto toResponse(
        Notification notification
    ) {
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