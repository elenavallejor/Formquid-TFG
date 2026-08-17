package com.formquid.participation.controller;

import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para el envío y consulta de participaciones.
 *
 * El envío de respuestas es público porque los encuestados no necesitan
 * cuenta. La consulta de participaciones sí requiere permisos sobre la encuesta.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Participaciones",
    description = "Envío anónimo de respuestas y consulta privada de participaciones"
)
public class ParticipationController {

    private final ParticipationService participationService;

    /**
     * Envía una participación anónima a una encuesta pública o privada.
     *
     * En encuestas privadas debe enviarse accessPassword dentro del cuerpo
     * de la petición.
     */
    @PostMapping("/public/surveys/{publicCode}/participations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Enviar participación",
        description = "Registra respuestas anónimas para una encuesta pública o privada."
    )
    public ParticipationResponseDto submit(
        @PathVariable String publicCode,
        @Valid @RequestBody SubmitParticipationRequestDto request
    ) {
        return participationService.submit(publicCode, request);
    }

    /**
     * Lista las participaciones de una encuesta.
     *
     * Pueden acceder el propietario, colaboradores, administradores
     * y superadministradores.
     */
    @GetMapping("/surveys/{surveyId}/participations")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar participaciones de una encuesta",
        description = "Devuelve las participaciones registradas si el usuario tiene permisos."
    )
    public List<ParticipationResponseDto> getBySurveyId(
        @PathVariable Long surveyId,
        Principal principal
    ) {
        return participationService.getBySurveyId(
            surveyId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Obtiene el correo del usuario autenticado almacenado en Principal.
     */
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
