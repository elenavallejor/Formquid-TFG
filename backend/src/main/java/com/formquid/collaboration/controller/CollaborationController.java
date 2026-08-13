package com.formquid.collaboration.controller;

import com.formquid.collaboration.dto.CollaborationRequestResponseDto;
import com.formquid.collaboration.dto.CreateCollaborationRequestDto;
import com.formquid.collaboration.dto.SurveyCollaboratorResponseDto;
import com.formquid.collaboration.service.CollaborationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para gestionar solicitudes de colaboración
 * y colaboradores de encuestas.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Colaboraciones",
    description = "Invitaciones y colaboradores de encuestas"
)
public class CollaborationController {

    private final CollaborationService collaborationService;

    /**
     * Crea una solicitud de colaboración para una encuesta.
     */
    @PostMapping("/surveys/{surveyId}/collaboration-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Invitar colaborador",
        description = "Crea una solicitud pendiente para que un editor colabore en una encuesta."
    )
    public CollaborationRequestResponseDto createRequest(
        @PathVariable Long surveyId,
        @Valid @RequestBody CreateCollaborationRequestDto request,
        Principal principal
    ) {
        return collaborationService.createRequest(
            surveyId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Lista las solicitudes pendientes de una encuesta.
     */
    @GetMapping("/surveys/{surveyId}/collaboration-requests")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar solicitudes pendientes",
        description = "Devuelve las solicitudes de colaboración pendientes de una encuesta."
    )
    public List<CollaborationRequestResponseDto> getPendingRequests(
        @PathVariable Long surveyId,
        Principal principal
    ) {
        return collaborationService.getPendingRequests(
            surveyId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Lista las solicitudes recibidas por el usuario autenticado.
     */
    @GetMapping("/collaboration-requests/received")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar invitaciones recibidas",
        description = "Devuelve las solicitudes de colaboración recibidas por el usuario autenticado."
    )
    public List<CollaborationRequestResponseDto> getReceivedRequests(
        Principal principal
    ) {
        return collaborationService.getReceivedRequests(
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Acepta una solicitud de colaboración recibida.
     */
    @PostMapping("/collaboration-requests/{requestId}/accept")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Aceptar invitación",
        description = "Acepta una solicitud de colaboración recibida."
    )
    public SurveyCollaboratorResponseDto accept(
        @PathVariable Long requestId,
        Principal principal
    ) {
        return collaborationService.accept(
            requestId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Rechaza una solicitud de colaboración recibida.
     */
    @PostMapping("/collaboration-requests/{requestId}/reject")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Rechazar invitación",
        description = "Rechaza una solicitud de colaboración recibida."
    )
    public CollaborationRequestResponseDto reject(
        @PathVariable Long requestId,
        Principal principal
    ) {
        return collaborationService.reject(
            requestId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Lista los colaboradores actuales de una encuesta.
     */
    @GetMapping("/surveys/{surveyId}/collaborators")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar colaboradores",
        description = "Devuelve los colaboradores actuales de una encuesta."
    )
    public List<SurveyCollaboratorResponseDto> getCollaborators(
        @PathVariable Long surveyId,
        Principal principal
    ) {
        return collaborationService.getCollaborators(
            surveyId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Elimina un colaborador de una encuesta.
     */
    @DeleteMapping("/surveys/{surveyId}/collaborators/{editorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Eliminar colaborador",
        description = "Elimina un editor colaborador de una encuesta."
    )
    public void removeCollaborator(
        @PathVariable Long surveyId,
        @PathVariable Long editorId,
        Principal principal
    ) {
        collaborationService.removeCollaborator(
            surveyId,
            editorId,
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
