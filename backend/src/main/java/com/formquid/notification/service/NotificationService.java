package com.formquid.notification.service;

import com.formquid.collaboration.entity.SurveyCollaboration;
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
import com.formquid.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Servicio de negocio para la gestión de notificaciones.
 *
 * Sus responsabilidades principales son:
 * - crear notificaciones internas o de correo;
 * - proporcionar helpers de dominio a otros módulos;
 * - consultar el historial del usuario autenticado;
 * - gestionar el estado de lectura.
 *
 * Las consultas son de solo lectura por defecto y únicamente los métodos
 * que modifican información abren una transacción de escritura.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * Crea una notificación interna para un usuario.
     *
     * Este método constituye la API genérica utilizada por otros módulos
     * cuando no existe un helper de dominio más específico.
     */
    @Transactional
    public NotificationResponseDto notifyInternal(
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        NotificationTargetType targetType,
        Long targetId
    ) {
        return createNotification(
            recipientId,
            type,
            NotificationChannel.INTERNAL,
            title,
            message,
            targetType,
            targetId
        );
    }

    /**
     * Registra una notificación asociada al canal de correo.
     *
     * El envío real del correo puede integrarse posteriormente con un
     * proveedor externo sin modificar la forma en que otros módulos
     * registran el evento.
     */
    @Transactional
    public NotificationResponseDto notifyEmail(
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        NotificationTargetType targetType,
        Long targetId
    ) {
        return createNotification(
            recipientId,
            type,
            NotificationChannel.EMAIL,
            title,
            message,
            targetType,
            targetId
        );
    }

    /**
     * Notifica al nuevo editor que su solicitud de registro ha sido aprobada.
     *
     * La notificación se asocia a la solicitud de registro que originó el alta
     * para conservar la trazabilidad del evento mientras dicha solicitud exista.
     */
    @Transactional
    public void notifyRegistrationApproved(
        Long recipientId,
        Long registrationRequestId
    ) {
        createNotification(
            recipientId,
            NotificationType.REGISTRATION_APPROVED,
            NotificationChannel.INTERNAL,
            "Solicitud de registro aprobada",
            "Tu solicitud de registro ha sido aprobada. Ya puedes acceder a FormQuid.",
            NotificationTargetType.REGISTRATION_REQUEST,
            registrationRequestId
        );
    }

    /**
     * Notifica a un editor que ha recibido una invitación
     * para colaborar en una encuesta.
     *
     * CollaborationService únicamente aporta los datos del evento y
     * desconoce cómo se construye internamente la notificación.
     */
    @Transactional
    public void notifyCollaborationInvite(
        Long recipientId,
        Long requestId,
        String surveyTitle
    ) {
        createNotification(
            recipientId,
            NotificationType.COLLABORATION_REQUEST,
            NotificationChannel.INTERNAL,
            "Nueva invitación de colaboración",
            "Has recibido una invitación para colaborar en la encuesta: "
                + surveyTitle,
            NotificationTargetType.COLLABORATION_REQUEST,
            requestId
        );
    }

    /**
     * Notifica al solicitante que una invitación de colaboración
     * ha sido aceptada.
     */
    @Transactional
    public void notifyCollaborationAccepted(
        Long recipientId,
        Long surveyId,
        String editorName,
        String surveyTitle
    ) {
        createNotification(
            recipientId,
            NotificationType.COLLABORATION_ACCEPTED,
            NotificationChannel.INTERNAL,
            "Invitación de colaboración aceptada",
            editorName
                + " ha aceptado colaborar en la encuesta: "
                + surveyTitle,
            NotificationTargetType.SURVEY,
            surveyId
        );
    }

    /**
     * Notifica al solicitante que una invitación de colaboración
     * ha sido rechazada.
     */
    @Transactional
    public void notifyCollaborationRejected(
        Long recipientId,
        Long surveyId,
        String editorName,
        String surveyTitle
    ) {
        createNotification(
            recipientId,
            NotificationType.COLLABORATION_REJECTED,
            NotificationChannel.INTERNAL,
            "Invitación de colaboración rechazada",
            editorName
                + " ha rechazado colaborar en la encuesta: "
                + surveyTitle,
            NotificationTargetType.SURVEY,
            surveyId
        );
    }

    /**
     * Notifica a un editor que ha dejado de formar parte
     * de los colaboradores de una encuesta.
     */
    @Transactional
    public void notifyCollaboratorRemoved(
        Long recipientId,
        Long surveyId,
        String surveyTitle
    ) {
        createNotification(
            recipientId,
            NotificationType.GENERAL,
            NotificationChannel.INTERNAL,
            "Colaboración eliminada",
            "Has dejado de colaborar en la encuesta: "
                + surveyTitle,
            NotificationTargetType.SURVEY,
            surveyId
        );
    }

    /**
     * Obtiene el historial paginado de notificaciones
     * del usuario autenticado.
     */
    public Page<NotificationResponseDto> getMyNotifications(
        String userEmail,
        Pageable pageable
    ) {
        User user = getAuthenticatedUser(userEmail);

        return notificationRepository
            .findByRecipientId(
                user.getId(),
                pageable
            )
            .map(this::toResponse);
    }

    /**
     * Obtiene de forma paginada únicamente las notificaciones
     * pendientes de lectura del usuario autenticado.
     */
    public Page<NotificationResponseDto> getUnreadNotifications(
        String userEmail,
        Pageable pageable
    ) {
        User user = getAuthenticatedUser(userEmail);

        return notificationRepository
            .findByRecipientIdAndReadAtIsNull(
                user.getId(),
                pageable
            )
            .map(this::toResponse);
    }

    /**
     * Devuelve el número de notificaciones todavía no leídas.
     */
    public long getUnreadCount(
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);

        return notificationRepository
            .countByRecipientIdAndReadAtIsNull(
                user.getId()
            );
    }

    /**
     * Marca una notificación concreta como leída.
     *
     * Solo el usuario destinatario puede realizar la operación.
     * El cambio de estado se encuentra encapsulado en Notification.markAsRead().
     */
    @Transactional
    public NotificationResponseDto markAsRead(
        Long notificationId,
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);
        Notification notification =
            getNotification(notificationId);

        validateNotificationOwner(
            notification,
            user.getId()
        );

        notification.markAsRead();

        return toResponse(notification);
    }

    /**
     * Marca todas las notificaciones pendientes del usuario como leídas.
     *
     * El repositorio ejecuta un único UPDATE masivo en PostgreSQL.
     *
     * @return número de notificaciones actualizadas.
     */
    @Transactional
    public int markAllAsRead(
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);

        return notificationRepository
            .markAllAsRead(
                user.getId(),
                LocalDateTime.now()
            );
    }

    /**
     * Centraliza la creación y persistencia de una notificación.
     *
     * Todos los métodos de creación terminan pasando por este punto,
     * garantizando validaciones y valores por defecto coherentes.
     */
    private NotificationResponseDto createNotification(
        Long recipientId,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String message,
        NotificationTargetType targetType,
        Long targetId
    ) {
        User recipient = getRecipient(recipientId);

        validateText(
            title,
            message
        );
        validateTarget(
            targetType,
            targetId
        );

        Notification.NotificationBuilder builder =
            Notification.builder()
                .recipient(recipient)
                .type(type != null ? type : NotificationType.GENERAL)
                .channel(channel != null ? channel : NotificationChannel.INTERNAL)
                .title(title.trim())
                .message(message.trim());

        assignTarget(
            builder,
            targetType,
            targetId
        );

        Notification notification = builder.build();

        return toResponse(
            notificationRepository.save(notification)
        );
    }

    private void validateTarget(
        NotificationTargetType targetType,
        Long targetId
    ) {
        NotificationTargetType effectiveTargetType =
            targetType != null
                ? targetType
                : NotificationTargetType.NONE;

        if (effectiveTargetType == NotificationTargetType.NONE && targetId != null) {
            throw new IllegalArgumentException(
                "Las notificaciones sin recurso asociado no deben indicar targetId"
            );
        }

        if (effectiveTargetType != NotificationTargetType.NONE && targetId == null) {
            throw new IllegalArgumentException(
                "Las notificaciones asociadas a un recurso deben indicar targetId"
            );
        }
    }

    private void assignTarget(
        Notification.NotificationBuilder builder,
        NotificationTargetType targetType,
        Long targetId
    ) {
        NotificationTargetType effectiveTargetType =
            targetType != null ? targetType : NotificationTargetType.NONE;

        switch (effectiveTargetType) {
            case NONE -> {
            }
            case SURVEY -> builder.survey(
                entityManager.getReference(Survey.class, targetId)
            );
            case REGISTRATION_REQUEST -> builder.registrationRequest(
                entityManager.getReference(RegistrationRequest.class, targetId)
            );
            case COLLABORATION_REQUEST -> builder.collaboration(
                entityManager.getReference(SurveyCollaboration.class, targetId)
            );
            case ELEMENT_REQUEST -> builder.elementRequest(
                entityManager.getReference(ElementRequest.class, targetId)
            );
            case USER -> builder.targetUser(
                entityManager.getReference(User.class, targetId)
            );
        }
    }

    /**
     * Obtiene una notificación por identificador.
     */
    private Notification getNotification(
        Long notificationId
    ) {
        return notificationRepository
            .findById(notificationId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la notificación indicada"
                )
            );
    }

    /**
     * Recupera el usuario autenticado mediante el correo almacenado
     * en java.security.Principal.
     */
    private User getAuthenticatedUser(
        String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return userRepository
            .findByEmailIgnoreCase(email.trim())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario autenticado no encontrado"
                )
            );
    }

    /**
     * Recupera el destinatario antes de crear una notificación.
     *
     * La comprobación explícita permite devolver un error de dominio
     * comprensible en lugar de depender de una restricción de clave foránea.
     */
    private User getRecipient(
        Long recipientId
    ) {
        if (recipientId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El destinatario de la notificación es obligatorio"
            );
        }

        return userRepository
            .findById(recipientId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el destinatario de la notificación"
                )
            );
    }

    /**
     * Comprueba que la notificación pertenece al usuario autenticado.
     */
    private void validateNotificationOwner(
        Notification notification,
        Long userId
    ) {
        if (
            notification.getRecipient() == null
            || notification.getRecipient().getId() == null
            || !notification.getRecipient()
                .getId()
                .equals(userId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No puedes modificar una notificación de otro usuario"
            );
        }
    }

    /**
     * Valida los textos obligatorios antes de persistir.
     */
    private void validateText(
        String title,
        String message
    ) {
        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El título de la notificación es obligatorio"
            );
        }

        if (!StringUtils.hasText(message)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El mensaje de la notificación es obligatorio"
            );
        }
    }

    /**
     * Convierte una entidad Notification en su DTO de salida.
     *
     * El booleano read se deriva de readAt para evitar mantener
     * dos estados de lectura que puedan quedar desincronizados.
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
            resolveTargetType(notification),
            resolveTargetId(notification),
            notification.getReadAt() != null,
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }

    private NotificationTargetType resolveTargetType(Notification notification) {
        if (notification.getSurvey() != null) {
            return NotificationTargetType.SURVEY;
        }

        if (notification.getRegistrationRequest() != null) {
            return NotificationTargetType.REGISTRATION_REQUEST;
        }

        if (notification.getCollaboration() != null) {
            return NotificationTargetType.COLLABORATION_REQUEST;
        }

        if (notification.getElementRequest() != null) {
            return NotificationTargetType.ELEMENT_REQUEST;
        }

        if (notification.getTargetUser() != null) {
            return NotificationTargetType.USER;
        }

        return NotificationTargetType.NONE;
    }

    private Long resolveTargetId(Notification notification) {
        if (notification.getSurvey() != null) {
            return notification.getSurvey().getId();
        }

        if (notification.getRegistrationRequest() != null) {
            return notification.getRegistrationRequest().getId();
        }

        if (notification.getCollaboration() != null) {
            return notification.getCollaboration().getId();
        }

        if (notification.getElementRequest() != null) {
            return notification.getElementRequest().getId();
        }

        if (notification.getTargetUser() != null) {
            return notification.getTargetUser().getId();
        }

        return null;
    }
}
