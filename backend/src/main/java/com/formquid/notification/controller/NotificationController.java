package com.formquid.notification.controller;

import com.formquid.common.dto.CountResponseDto;
import com.formquid.notification.dto.NotificationResponseDto;
import com.formquid.notification.dto.UnreadNotificationCountResponseDto;
import com.formquid.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Controlador REST para la consulta y gestión de notificaciones.
 *
 * Todas las operaciones actúan exclusivamente sobre las notificaciones
 * pertenecientes al usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Notificaciones",
    description = "Consulta y actualización de notificaciones internas"
)
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Obtiene de forma paginada las notificaciones del usuario autenticado.
     *
     * Por defecto se devuelven 20 elementos ordenados desde el más reciente.
     */
    @GetMapping
    @Operation(
        summary = "Listar mis notificaciones",
        description = "Devuelve el historial paginado de notificaciones del usuario autenticado."
    )
    public Page<NotificationResponseDto> getMyNotifications(
        Principal principal,
        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {
        return notificationService.getMyNotifications(
            getAuthenticatedEmail(principal),
            pageable
        );
    }

    /**
     * Obtiene de forma paginada únicamente las notificaciones no leídas.
     */
    @GetMapping("/unread")
    @Operation(
        summary = "Listar notificaciones no leídas",
        description = "Devuelve las notificaciones pendientes de lectura del usuario autenticado."
    )
    public Page<NotificationResponseDto> getUnreadNotifications(
        Principal principal,
        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        )
        Pageable pageable
    ) {
        return notificationService.getUnreadNotifications(
            getAuthenticatedEmail(principal),
            pageable
        );
    }

    /**
     * Obtiene el número de notificaciones pendientes de lectura.
     */
    @GetMapping("/unread/count")
    @Operation(
        summary = "Contar notificaciones no leídas",
        description = "Devuelve el número de notificaciones pendientes de lectura."
    )
    public UnreadNotificationCountResponseDto getUnreadCount(
        Principal principal
    ) {
        return new UnreadNotificationCountResponseDto(
            notificationService.getUnreadCount(
                getAuthenticatedEmail(principal)
            )
        );
    }

    /**
     * Marca una notificación concreta como leída.
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(
        summary = "Marcar notificación como leída",
        description = "Marca como leída una notificación del usuario autenticado."
    )
    public NotificationResponseDto markAsRead(
        @PathVariable Long notificationId,
        Principal principal
    ) {
        return notificationService.markAsRead(
            notificationId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Marca todas las notificaciones pendientes como leídas.
     *
     * Devuelve el número de filas modificadas por la actualización masiva.
     */
    @PatchMapping("/read-all")
    @Operation(
        summary = "Marcar todas como leídas",
        description = "Marca en una sola operación todas las notificaciones pendientes del usuario."
    )
    public CountResponseDto markAllAsRead(
        Principal principal
    ) {
        return new CountResponseDto(
            notificationService.markAllAsRead(
                getAuthenticatedEmail(principal)
            )
        );
    }

    /**
     * Obtiene el correo del usuario autenticado.
     */
    private String getAuthenticatedEmail(
        Principal principal
    ) {
        if (
            principal == null
            || principal.getName() == null
            || principal.getName().isBlank()
        ) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado"
            );
        }

        return principal.getName();
    }
}
