package com.formquid.template.controller;

import com.formquid.template.dto.CreateSurveyTemplateRequestDto;
import com.formquid.template.dto.SurveyTemplateResponseDto;
import com.formquid.template.dto.UpdateSurveyTemplateRequestDto;
import com.formquid.template.service.SurveyTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para la gestión de plantillas reutilizables.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SurveyTemplateController {

    private final SurveyTemplateService surveyTemplateService;

    /**
     * Obtiene las plantillas activas disponibles para crear encuestas.
     */
    @GetMapping("/templates/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    public List<SurveyTemplateResponseDto> findActive() {
        return surveyTemplateService.findActive();
    }

    /**
     * Obtiene todas las plantillas desde el panel de administración.
     */
    @GetMapping("/admin/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public List<SurveyTemplateResponseDto> findAll() {
        return surveyTemplateService.findAll();
    }

    /**
     * Obtiene una plantilla concreta por su identificador.
     */
    @GetMapping("/admin/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto findById(
        @PathVariable Long id
    ) {
        return surveyTemplateService.findById(id);
    }

    /**
     * Crea una nueva plantilla reutilizable.
     */
    @PostMapping("/admin/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto create(
        @Valid @RequestBody CreateSurveyTemplateRequestDto request,
        Principal principal
    ) {
        return surveyTemplateService.create(
            request,
            principal.getName()
        );
    }

    /**
     * Actualiza una plantilla existente sustituyendo su estructura completa.
     */
    @PutMapping("/admin/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateSurveyTemplateRequestDto request
    ) {
        return surveyTemplateService.update(id, request);
    }

    /**
     * Activa una plantilla.
     */
    @PatchMapping("/admin/templates/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto activate(
        @PathVariable Long id
    ) {
        return surveyTemplateService.activate(id);
    }

    /**
     * Desactiva una plantilla.
     */
    @PatchMapping("/admin/templates/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto disable(
        @PathVariable Long id
    ) {
        return surveyTemplateService.disable(id);
    }

    /**
     * Archiva una plantilla.
     */
    @PatchMapping("/admin/templates/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public SurveyTemplateResponseDto archive(
        @PathVariable Long id
    ) {
        return surveyTemplateService.archive(id);
    }
}
