package com.formquid.registration.controller;

import com.formquid.auth.security.CustomUserDetails;
import com.formquid.registration.dto.CreateRegistrationRequestDto;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.dto.ResolveRegistrationRequestDto;
import com.formquid.registration.service.RegistrationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar solicitudes de registro de editores.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Solicitudes de registro",
    description = "Creación y resolución de solicitudes de alta como editor"
)
public class RegistrationRequestController {

    private final RegistrationRequestService registrationRequestService;

    /**
     * Crea una solicitud pública de registro como editor.
     *
     * @param request datos del solicitante.
     * @return solicitud creada.
     */
    @PostMapping("/registration-requests")
    @Operation(
        summary = "Solicitar registro como editor",
        description = "Permite a una persona solicitar acceso a la plataforma con rol de editor."
    )
    public RegistrationRequestResponseDto create(
        @Valid @RequestBody CreateRegistrationRequestDto request
    ) {
        return registrationRequestService.create(request);
    }

    /**
     * Obtiene todas las solicitudes de registro.
     *
     * @return lista de solicitudes.
     */
    @GetMapping("/admin/registration-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar solicitudes de registro",
        description = "Devuelve todas las solicitudes de registro existentes."
    )
    public List<RegistrationRequestResponseDto> findAll() {
        return registrationRequestService.findAll();
    }

    /**
     * Obtiene las solicitudes de registro pendientes.
     *
     * @return lista de solicitudes pendientes.
     */
    @GetMapping("/admin/registration-requests/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar solicitudes pendientes",
        description = "Devuelve las solicitudes de registro pendientes de revisión."
    )
    public List<RegistrationRequestResponseDto> findPending() {
        return registrationRequestService.findPending();
    }

    /**
     * Aprueba una solicitud y crea un usuario editor.
     *
     * @param id identificador de la solicitud.
     * @param userDetails usuario autenticado que resuelve la solicitud.
     * @return solicitud aprobada.
     */
    @PostMapping("/admin/registration-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Aprobar solicitud de registro",
        description = "Aprueba una solicitud pendiente y crea una cuenta de usuario con rol EDITOR."
    )
    public RegistrationRequestResponseDto approve(
        @PathVariable Long id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return registrationRequestService.approve(id, userDetails.getId());
    }

    /**
     * Rechaza una solicitud pendiente.
     *
     * @param id identificador de la solicitud.
     * @param request datos opcionales de rechazo.
     * @param userDetails usuario autenticado que resuelve la solicitud.
     * @return solicitud rechazada.
     */
    @PostMapping("/admin/registration-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Rechazar solicitud de registro",
        description = "Marca una solicitud pendiente como rechazada."
    )
    public RegistrationRequestResponseDto reject(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ResolveRegistrationRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return registrationRequestService.reject(id, userDetails.getId(), request);
    }
}