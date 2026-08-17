package com.formquid.elementrequest.controller;

import com.formquid.common.dto.CountResponseDto;
import com.formquid.elementrequest.dto.ApproveElementRequestResponseDto;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Controlador reservado a la gestión administrativa de solicitudes
 * pendientes de elementos reutilizables.
 */
@RestController
@RequestMapping("/api/v1/admin/element-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Administración de solicitudes de elementos",
    description = "Consulta y resolución administrativa de solicitudes pendientes"
)
public class ElementRequestAdminController {

    private final ElementRequestService elementRequestService;

    /** Lista todas las solicitudes existentes; todas ellas son pendientes. */
    @GetMapping
    public List<ElementRequestResponseDto> getAll() {
        return elementRequestService.getAll();
    }

    /** Obtiene una solicitud pendiente concreta. */
    @GetMapping("/{requestId}")
    public ElementRequestResponseDto getById(@PathVariable Long requestId) {
        return elementRequestService.getById(requestId);
    }

    /** Alias semántico para listar solicitudes pendientes. */
    @GetMapping("/pending")
    public List<ElementRequestResponseDto> getPending() {
        return elementRequestService.getPending();
    }

    /** Cuenta las solicitudes pendientes. */
    @GetMapping("/pending/count")
    public CountResponseDto countPending() {
        return new CountResponseDto(elementRequestService.countPending());
    }

    /**
     * Aprueba la solicitud, crea el elemento solicitado y conserva
     * el registro resuelto para auditoría.
     */
    @PatchMapping("/{requestId}/approve")
    @Operation(
        summary = "Aprobar solicitud",
        description = "Crea el elemento solicitado, notifica al editor y marca la solicitud como aprobada."
    )
    public ApproveElementRequestResponseDto approve(
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false) ResolveElementRequestDto request,
        Principal principal
    ) {
        return elementRequestService.approve(
            requestId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Rechaza la solicitud, notifica al editor y conserva
     * el registro resuelto para auditoría.
     */
    @PatchMapping("/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Rechazar solicitud",
        description = "Notifica al solicitante y marca la solicitud como rechazada sin crear ningún elemento."
    )
    public void reject(
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false) ResolveElementRequestDto request,
        Principal principal
    ) {
        elementRequestService.reject(
            requestId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /** Obtiene el correo del administrador autenticado. */
    private String getAuthenticatedEmail(Principal principal) {
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
