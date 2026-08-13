package com.formquid.fixedquestion.controller;

import com.formquid.fixedquestion.dto.CreateFixedQuestionRequestDto;
import com.formquid.fixedquestion.dto.FixedQuestionResponseDto;
import com.formquid.fixedquestion.dto.UpdateFixedQuestionRequestDto;
import com.formquid.fixedquestion.service.FixedQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para gestionar preguntas fijas reutilizables.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Preguntas fijas",
    description = "Gestión de preguntas fijas reutilizables"
)
public class FixedQuestionController {

    private final FixedQuestionService fixedQuestionService;

    /**
     * Devuelve las preguntas fijas activas.
     */
    @GetMapping("/fixed-questions/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas fijas activas",
        description = "Devuelve las preguntas fijas activas disponibles para su uso."
    )
    public List<FixedQuestionResponseDto> findActive() {
        return fixedQuestionService.findActive();
    }

    /**
     * Devuelve las preguntas activas asociadas a un bloque.
     */
    @GetMapping("/blocks/{blockId}/fixed-questions/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas activas por bloque",
        description = "Devuelve las preguntas fijas activas asociadas a un bloque."
    )
    public List<FixedQuestionResponseDto> findActiveByBlock(
        @PathVariable Long blockId
    ) {
        return fixedQuestionService.findActiveByBlock(blockId);
    }

    /**
     * Devuelve todas las preguntas fijas.
     */
    @GetMapping("/admin/fixed-questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas fijas",
        description = "Devuelve todas las preguntas fijas registradas."
    )
    public List<FixedQuestionResponseDto> findAll() {
        return fixedQuestionService.findAll();
    }

    /**
     * Devuelve una pregunta fija por id.
     */
    @GetMapping("/admin/fixed-questions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Obtener pregunta fija",
        description = "Devuelve la información de una pregunta fija concreta."
    )
    public FixedQuestionResponseDto findById(
        @PathVariable Long id
    ) {
        return fixedQuestionService.findById(id);
    }

    /**
     * Crea una nueva pregunta fija.
     */
    @PostMapping("/admin/fixed-questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Crear pregunta fija",
        description = "Crea una nueva pregunta fija reutilizable."
    )
    public FixedQuestionResponseDto create(
        @Valid @RequestBody CreateFixedQuestionRequestDto request,
        Principal principal
    ) {
        return fixedQuestionService.create(
            request,
            principal.getName()
        );
    }

    /**
     * Actualiza una pregunta fija.
     */
    @PutMapping("/admin/fixed-questions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Actualizar pregunta fija",
        description = "Modifica los datos de una pregunta fija existente."
    )
    public FixedQuestionResponseDto update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateFixedQuestionRequestDto request
    ) {
        return fixedQuestionService.update(id, request);
    }

    /**
     * Activa una pregunta fija.
     */
    @PatchMapping("/admin/fixed-questions/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Activar pregunta fija",
        description = "Marca una pregunta fija como activa."
    )
    public FixedQuestionResponseDto activate(
        @PathVariable Long id
    ) {
        return fixedQuestionService.activate(id);
    }

    /**
     * Desactiva una pregunta fija.
     */
    @PatchMapping("/admin/fixed-questions/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Desactivar pregunta fija",
        description = "Marca una pregunta fija como desactivada."
    )
    public FixedQuestionResponseDto disable(
        @PathVariable Long id
    ) {
        return fixedQuestionService.disable(id);
    }

    /**
     * Archiva una pregunta fija.
     */
    @PatchMapping("/admin/fixed-questions/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Archivar pregunta fija",
        description = "Marca una pregunta fija como archivada."
    )
    public FixedQuestionResponseDto archive(
        @PathVariable Long id
    ) {
        return fixedQuestionService.archive(id);
    }
}