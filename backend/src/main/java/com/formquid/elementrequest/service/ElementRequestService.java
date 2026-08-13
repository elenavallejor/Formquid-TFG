package com.formquid.elementrequest.service;

import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.dto.ResolveElementRequestDto;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.ElementRequestStatus;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.service.NotificationService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de negocio para la gestión de solicitudes de elementos.
 *
 * Agrupa tanto las operaciones de los editores como las operaciones
 * administrativas, manteniendo la autorización HTTP separada en
 * dos controladores diferentes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ElementRequestService {

    private final ElementRequestRepository elementRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Crea una nueva solicitud de elemento reutilizable.
     *
     * El usuario autenticado se obtiene a partir de su correo, evitando
     * depender de una implementación personalizada de UserDetails.
     */
    @Transactional
    public ElementRequestResponseDto create(
        CreateElementRequestDto request,
        String requesterEmail
    ) {
        User requester = getAuthenticatedUser(requesterEmail);

        ElementRequest elementRequest =
            ElementRequest.builder()
                .type(request.type())
                .title(request.title().trim())
                .description(request.description().trim())
                .justification(
                    cleanOptional(request.justification())
                )
                .requestedBy(requester)
                .status(ElementRequestStatus.PENDING)
                .build();

        return toResponse(
            elementRequestRepository.save(elementRequest)
        );
    }

    /**
     * Obtiene todas las solicitudes ordenadas por fecha de creación.
     *
     * Este método se utiliza desde el controlador administrativo.
     */
    public List<ElementRequestResponseDto> getAll() {
        return elementRequestRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene una solicitud concreta por su identificador.
     *
     * El repositorio precarga requestedBy y resolvedBy mediante
     * EntityGraph para evitar consultas adicionales al construir el DTO.
     */
    public ElementRequestResponseDto getById(Long requestId) {
        return toResponse(
            getRequest(requestId)
        );
    }

    /**
     * Obtiene únicamente las solicitudes todavía pendientes.
     */
    public List<ElementRequestResponseDto> getPending() {
        return elementRequestRepository
            .findByStatusOrderByCreatedAtDesc(
                ElementRequestStatus.PENDING
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene el historial de solicitudes creadas
     * por el usuario autenticado.
     */
    public List<ElementRequestResponseDto> getMyRequests(
        String userEmail
    ) {
        User user = getAuthenticatedUser(userEmail);

        return elementRequestRepository
            .findByRequestedByIdOrderByCreatedAtDesc(
                user.getId()
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Cuenta las solicitudes todavía pendientes.
     *
     * La operación se ejecuta directamente mediante COUNT en base de datos,
     * sin cargar las entidades en memoria.
     */
    public long countPending() {
        return elementRequestRepository
            .countByStatus(
                ElementRequestStatus.PENDING
            );
    }

    /**
     * Aprueba una solicitud pendiente.
     *
     * La transición y los campos de auditoría se establecen dentro
     * de ElementRequest para garantizar consistencia.
     */
    @Transactional
    public ElementRequestResponseDto approve(
        Long requestId,
        ResolveElementRequestDto request,
        String adminEmail
    ) {
        User admin = getAdministrator(adminEmail);
        ElementRequest elementRequest = getRequest(requestId);

        /*
         * La entidad comprueba por sí misma que siga pendiente.
         */
        elementRequest.approve(
            admin,
            request.resolutionMessage()
        );

        notifyRequester(
            elementRequest,
            true
        );

        return toResponse(elementRequest);
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * La entidad impide resolver dos veces la misma solicitud.
     */
    @Transactional
    public ElementRequestResponseDto reject(
        Long requestId,
        ResolveElementRequestDto request,
        String adminEmail
    ) {
        User admin = getAdministrator(adminEmail);
        ElementRequest elementRequest = getRequest(requestId);

        elementRequest.reject(
            admin,
            request.resolutionMessage()
        );

        notifyRequester(
            elementRequest,
            false
        );

        return toResponse(elementRequest);
    }

    /**
     * Recupera una solicitud o devuelve un error 404.
     */
    private ElementRequest getRequest(Long requestId) {
        return elementRequestRepository
            .findById(requestId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe la solicitud indicada"
                )
            );
    }

    /**
     * Recupera el usuario autenticado mediante el correo almacenado
     * en java.security.Principal.
     */
    private User getAuthenticatedUser(String email) {
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
     * Recupera y valida al usuario que intenta resolver una solicitud.
     *
     * Aunque los endpoints administrativos están protegidos mediante
     * @PreAuthorize, esta comprobación mantiene segura la capa de negocio
     * si el servicio se reutiliza desde otro componente.
     */
    private User getAdministrator(String email) {
        User user = getAuthenticatedUser(email);

        if (
            user.getRole() != UserRole.ADMIN
            && user.getRole() != UserRole.SUPERADMIN
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Solo un administrador puede resolver solicitudes de elementos"
            );
        }

        return user;
    }

    /**
     * Notifica al solicitante el resultado de la revisión.
     */
    private void notifyRequester(
        ElementRequest request,
        boolean approved
    ) {
        String title = approved
            ? "Solicitud de nuevo elemento aprobada"
            : "Solicitud de nuevo elemento rechazada";

        String message = approved
            ? "Tu solicitud de nuevo elemento ha sido aprobada por un administrador."
            : "Tu solicitud de nuevo elemento ha sido rechazada por un administrador.";

        notificationService.notifyInternal(
            request.getRequestedBy().getId(),
            NotificationType.ELEMENT_REQUEST_RESOLVED,
            title,
            message,
            NotificationTargetType.ELEMENT_REQUEST,
            request.getId()
        );
    }

    /**
     * Convierte una entidad a su DTO de salida.
     */
    private ElementRequestResponseDto toResponse(
        ElementRequest request
    ) {
        User requestedBy = request.getRequestedBy();
        User resolvedBy = request.getResolvedBy();

        return new ElementRequestResponseDto(
            request.getId(),
            request.getType(),
            request.getTitle(),
            request.getDescription(),
            request.getJustification(),
            requestedBy.getId(),
            requestedBy.getName(),
            request.getStatus(),
            resolvedBy != null
                ? resolvedBy.getId()
                : null,
            resolvedBy != null
                ? resolvedBy.getName()
                : null,
            request.getResolutionMessage(),
            request.getCreatedAt(),
            request.getResolvedAt()
        );
    }

    /**
     * Limpia un texto opcional.
     */
    private String cleanOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
