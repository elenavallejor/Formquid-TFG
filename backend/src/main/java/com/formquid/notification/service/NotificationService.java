package com.formquid.notification.service;

import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.repository.NotificationRepository;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
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
     * Notifica al nuevo editor que su solicitud de registro
     * ha sido aprobada.
     *
     * RegistrationRequestService únicamente aporta los identificadores
     * del evento y no necesita conocer tipos, títulos ni mensajes internos.
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
            "Tu solicitud de registro en FormQuid ha sido aprobada.",
            NotificationTargetType.REGISTRATION_REQUEST,
            registrationRequestId
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

        Notification notification =
            Notification.builder()
                .recipient(recipient)
                .type(
                    type != null
                        ? type
                        : NotificationType.GENERAL
                )
                .channel(
                    channel != null
                        ? channel
                        : NotificationChannel.INTERNAL
                )
                .title(title.trim())
                .message(message.trim())
                .targetType(
                    targetType != null
                        ? targetType
                        : NotificationTargetType.NONE
                )
                .targetId(targetId)
                .build();

        return toResponse(
            notificationRepository.save(notification)
        );
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
            notification.getTargetType(),
            notification.getTargetId(),
            notification.getReadAt() != null,
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}