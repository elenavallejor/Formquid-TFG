package com.formquid.elementrequest.controller;

import com.formquid.common.dto.CountResponseDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.dto.ResolveElementRequestDto;
import com.formquid.elementrequest.service.ElementRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST reservado a la gestión administrativa
 * de solicitudes de elementos reutilizables.
 */
@RestController
@RequestMapping("/api/v1/admin/element-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Administración de solicitudes de elementos",
    description = "Consulta y resolución administrativa de solicitudes"
)
public class ElementRequestAdminController {

    private final ElementRequestService elementRequestService;

    /**
     * Lista todas las solicitudes registradas.
     */
    @GetMapping
    @Operation(
        summary = "Listar solicitudes",
        description = "Devuelve todas las solicitudes ordenadas por fecha de creación."
    )
    public List<ElementRequestResponseDto> getAll() {
        return elementRequestService.getAll();
    }

    /**
     * Obtiene una solicitud concreta para su revisión.
     */
    @GetMapping("/{requestId}")
    @Operation(
        summary = "Obtener solicitud",
        description = "Devuelve el detalle de una solicitud de elemento."
    )
    public ElementRequestResponseDto getById(
        @PathVariable Long requestId
    ) {
        return elementRequestService.getById(
            requestId
        );
    }

    /**
     * Lista únicamente las solicitudes todavía pendientes.
     */
    @GetMapping("/pending")
    @Operation(
        summary = "Listar solicitudes pendientes",
        description = "Devuelve las solicitudes pendientes de resolución."
    )
    public List<ElementRequestResponseDto> getPending() {
        return elementRequestService.getPending();
    }

    /**
     * Cuenta las solicitudes todavía pendientes.
     */
    @GetMapping("/pending/count")
    @Operation(
        summary = "Contar solicitudes pendientes",
        description = "Devuelve el número de solicitudes pendientes."
    )
    public CountResponseDto countPending() {
        return new CountResponseDto(
            elementRequestService.countPending()
        );
    }

    /**
     * Aprueba una solicitud pendiente.
     *
     * La aprobación únicamente resuelve la propuesta; no crea
     * automáticamente el bloque, pregunta o plantilla solicitada.
     */
    @PatchMapping("/{requestId}/approve")
    @Operation(
        summary = "Aprobar solicitud",
        description = "Marca una solicitud pendiente como aprobada."
    )
    public ElementRequestResponseDto approve(
        @PathVariable Long requestId,
        @Valid @RequestBody ResolveElementRequestDto request,
        Principal principal
    ) {
        return elementRequestService.approve(
            requestId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Rechaza una solicitud pendiente.
     */
    @PatchMapping("/{requestId}/reject")
    @Operation(
        summary = "Rechazar solicitud",
        description = "Marca una solicitud pendiente como rechazada."
    )
    public ElementRequestResponseDto reject(
        @PathVariable Long requestId,
        @Valid @RequestBody ResolveElementRequestDto request,
        Principal principal
    ) {
        return elementRequestService.reject(
            requestId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Obtiene el correo del administrador autenticado.
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
