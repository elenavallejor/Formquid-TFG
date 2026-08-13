package com.formquid.registration.controller;

import com.formquid.registration.dto.PendingRegistrationRequestCountResponseDto;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.dto.ResolveRegistrationRequestDto;
import com.formquid.registration.service.RegistrationRequestService;
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
 * de solicitudes de registro.
 */
@RestController
@RequestMapping("/api/v1/admin/registration-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Administración de solicitudes de registro",
    description = "Consulta y resolución de solicitudes de alta como editor"
)
public class RegistrationRequestAdminController {

    private final RegistrationRequestService registrationRequestService;

    /**
     * Obtiene todas las solicitudes de registro.
     */
    @GetMapping
    @Operation(
        summary = "Listar solicitudes de registro",
        description = "Devuelve todas las solicitudes existentes."
    )
    public List<RegistrationRequestResponseDto> getAll() {
        return registrationRequestService.getAll();
    }

    /**
     * Obtiene únicamente las solicitudes pendientes.
     */
    @GetMapping("/pending")
    @Operation(
        summary = "Listar solicitudes pendientes",
        description = "Devuelve las solicitudes que todavía deben revisarse."
    )
    public List<RegistrationRequestResponseDto> getPending() {
        return registrationRequestService.getPending();
    }

    /**
     * Devuelve el número de solicitudes pendientes.
     */
    @GetMapping("/pending/count")
    @Operation(
        summary = "Contar solicitudes pendientes",
        description = "Devuelve el número de solicitudes pendientes de revisión."
    )
    public PendingRegistrationRequestCountResponseDto getPendingCount() {
        return new PendingRegistrationRequestCountResponseDto(
            registrationRequestService.getPendingCount()
        );
    }

    /**
     * Aprueba una solicitud pendiente y crea la cuenta EDITOR.
     */
    @PatchMapping("/{requestId}/approve")
    @Operation(
        summary = "Aprobar solicitud de registro",
        description = "Aprueba la solicitud y crea atómicamente la cuenta EDITOR."
    )
    public RegistrationRequestResponseDto approve(
        @PathVariable Long requestId,
        Principal principal
    ) {
        return registrationRequestService.approve(
            requestId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Rechaza una solicitud pendiente.
     */
    @PatchMapping("/{requestId}/reject")
    @Operation(
        summary = "Rechazar solicitud de registro",
        description = "Rechaza una solicitud pendiente y registra el motivo indicado."
    )
    public RegistrationRequestResponseDto reject(
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false)
        ResolveRegistrationRequestDto request,
        Principal principal
    ) {
        return registrationRequestService.reject(
            requestId,
            request,
            getAuthenticatedEmail(principal)
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
