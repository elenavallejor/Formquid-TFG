package com.formquid.elementrequest.controller;

import com.formquid.elementrequest.dto.CreateElementRequestDto;
import com.formquid.elementrequest.dto.ElementRequestResponseDto;
import com.formquid.elementrequest.service.ElementRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para las operaciones de solicitudes
 * disponibles a usuarios autenticados.
 *
 * Los editores pueden crear propuestas de nuevos elementos
 * y consultar exclusivamente sus propias solicitudes.
 */
@RestController
@RequestMapping("/api/v1/element-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Solicitudes de elementos",
    description = "Creación y consulta de solicitudes propias"
)
public class ElementRequestController {

    private final ElementRequestService elementRequestService;

    /**
     * Crea una nueva solicitud de elemento reutilizable.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Crear solicitud",
        description = "Permite proponer la creación de un bloque, pregunta fija o plantilla."
    )
    public ElementRequestResponseDto create(
        @Valid @RequestBody CreateElementRequestDto request,
        Principal principal
    ) {
        return elementRequestService.create(
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Obtiene el historial de solicitudes creadas
     * por el usuario autenticado.
     */
    @GetMapping("/mine")
    @Operation(
        summary = "Listar mis solicitudes",
        description = "Devuelve únicamente las solicitudes creadas por el usuario autenticado."
    )
    public List<ElementRequestResponseDto> getMyRequests(
        Principal principal
    ) {
        return elementRequestService.getMyRequests(
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
