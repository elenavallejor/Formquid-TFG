package com.formquid.notification.repository;

import com.formquid.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para notificaciones.
 *
 * Las consultas de historial utilizan paginación para evitar cargar
 * un número indefinido de notificaciones en memoria.
 */
public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

    /**
     * Busca una notificación cargando también su destinatario.
     *
     * La relación recipient se necesita al validar que únicamente
     * el propietario pueda marcar la notificación como leída.
     */
    @Override
    @EntityGraph(attributePaths = {"recipient"})
    Optional<Notification> findById(Long id);

    /**
     * Obtiene de forma paginada las notificaciones de un usuario.
     *
     * La ordenación se recibe mediante Pageable desde el controlador.
     */
    Page<Notification> findByRecipientId(
        Long recipientId,
        Pageable pageable
    );

    /**
     * Obtiene de forma paginada las notificaciones todavía no leídas.
     */
    Page<Notification> findByRecipientIdAndReadAtIsNull(
        Long recipientId,
        Pageable pageable
    );

    /**
     * Cuenta las notificaciones no leídas mediante COUNT en base de datos.
     */
    long countByRecipientIdAndReadAtIsNull(
        Long recipientId
    );

    /**
     * Marca en una única sentencia SQL todas las notificaciones
     * pendientes de un usuario como leídas.
     *
     * Esta operación evita recuperar entidades y ejecutar una actualización
     * individual para cada notificación.
     *
     * @return número de notificaciones actualizadas.
     */
    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("""
        update Notification notification
        set notification.readAt = :readAt
        where notification.recipient.id = :recipientId
          and notification.readAt is null
    """)
    int markAllAsRead(
        @Param("recipientId") Long recipientId,
        @Param("readAt") LocalDateTime readAt
    );
}
