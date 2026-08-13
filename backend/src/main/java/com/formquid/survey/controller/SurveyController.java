package com.formquid.survey.controller;

import com.formquid.survey.dto.ChangeSurveyStatusRequestDto;
import com.formquid.survey.dto.CreateSurveyFromTemplateRequestDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.SurveySummaryResponseDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.service.SurveyService;
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
 * Controlador REST para la gestión autenticada de encuestas.
 *
 * El acceso de participantes anónimos se encuentra separado
 * en PublicSurveyController.
 */
@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
@Tag(
    name = "Encuestas",
    description = "Creación, edición y gestión autenticada de encuestas"
)
public class SurveyController {

    private final SurveyService surveyService;

    /**
     * Crea una encuesta desde cero.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Crear encuesta",
        description = "Crea una nueva encuesta en estado borrador."
    )
    public SurveyResponseDto create(
        @Valid @RequestBody CreateSurveyRequestDto request,
        Principal principal
    ) {
        return surveyService.create(
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Crea una encuesta copiando una plantilla activa.
     */
    @PostMapping("/from-template")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Crear encuesta desde plantilla",
        description = "Crea una encuesta copiando la estructura de una plantilla activa."
    )
    public SurveyResponseDto createFromTemplate(
        @Valid @RequestBody CreateSurveyFromTemplateRequestDto request,
        Principal principal
    ) {
        return surveyService.createFromTemplate(
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Lista las encuestas creadas por el usuario autenticado.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar mis encuestas",
        description = "Devuelve las encuestas creadas por el usuario autenticado."
    )
    public List<SurveyResponseDto> findMine(
        Principal principal
    ) {
        return surveyService.findMine(
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Lista las encuestas en las que el usuario participa como colaborador.
     */
    @GetMapping("/collaborations")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar mis colaboraciones",
        description = "Devuelve las encuestas en las que el usuario autenticado es colaborador."
    )
    public List<SurveySummaryResponseDto> findCollaborations(
        Principal principal
    ) {
        return surveyService.findCollaboratingSurveys(
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Obtiene una encuesta si el usuario dispone de permisos.
     */
    @GetMapping("/{surveyId}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Obtener encuesta",
        description = "Devuelve una encuesta completa para su gestión."
    )
    public SurveyResponseDto findById(
        @PathVariable Long surveyId,
        Principal principal
    ) {
        return surveyService.findById(
            surveyId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Actualiza una encuesta en borrador.
     */
    @PutMapping("/{surveyId}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Actualizar encuesta",
        description = "Actualiza los datos, etiquetas y estructura de una encuesta en borrador."
    )
    public SurveyResponseDto update(
        @PathVariable Long surveyId,
        @Valid @RequestBody UpdateSurveyRequestDto request,
        Principal principal
    ) {
        return surveyService.update(
            surveyId,
            request,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Cambia el estado o visibilidad de una encuesta.
     */
    @PatchMapping("/{surveyId}/status")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Cambiar estado de encuesta",
        description = "Cambia el estado entre DRAFT, PUBLIC, PRIVATE y HIDDEN."
    )
    public SurveyResponseDto changeStatus(
        @PathVariable Long surveyId,
        @Valid @RequestBody ChangeSurveyStatusRequestDto request,
        Principal principal
    ) {
        return surveyService.changeStatus(
            surveyId,
            request,
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
