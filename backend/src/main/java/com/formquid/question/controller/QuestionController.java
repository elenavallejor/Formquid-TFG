package com.formquid.question.controller;

import com.formquid.question.dto.CreateQuestionRequestDto;
import com.formquid.question.dto.QuestionResponseDto;
import com.formquid.question.dto.UpdateQuestionRequestDto;
import com.formquid.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controlador REST para gestionar preguntas reutilizables.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(
    name = "Preguntas",
    description = "Gestión de preguntas reutilizables"
)
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Devuelve las preguntas activas.
     */
    @GetMapping("/questions/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas activas",
        description = "Devuelve las preguntas activas disponibles para su uso."
    )
    public List<QuestionResponseDto> findActive() {
        return questionService.findActive();
    }

    /**
     * Devuelve las preguntas activas asociadas a un bloque.
     */
    @GetMapping("/blocks/{blockId}/questions/active")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas activas por bloque",
        description = "Devuelve las preguntas activas asociadas a un bloque."
    )
    public List<QuestionResponseDto> findActiveByBlock(
        @PathVariable Long blockId
    ) {
        return questionService.findActiveByBlock(blockId);
    }

    /**
     * Devuelve todas las preguntas.
     */
    @GetMapping("/admin/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar preguntas",
        description = "Devuelve todas las preguntas registradas."
    )
    public List<QuestionResponseDto> findAll() {
        return questionService.findAll();
    }

    /**
     * Devuelve una pregunta por id.
     */
    @GetMapping("/admin/questions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Obtener pregunta",
        description = "Devuelve la información de una pregunta concreta."
    )
    public QuestionResponseDto findById(
        @PathVariable Long id
    ) {
        return questionService.findById(id);
    }

    /**
     * Crea una nueva pregunta.
     */
    @PostMapping("/admin/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Crear pregunta",
        description = "Crea una nueva pregunta reutilizable."
    )
    public QuestionResponseDto create(
        @Valid @RequestBody CreateQuestionRequestDto request,
        Principal principal
    ) {
        return questionService.create(
            request,
            principal.getName()
        );
    }

    /**
     * Actualiza una pregunta.
     */
    @PutMapping("/admin/questions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Actualizar pregunta",
        description = "Modifica los datos de una pregunta existente."
    )
    public QuestionResponseDto update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateQuestionRequestDto request
    ) {
        return questionService.update(id, request);
    }

    /**
     * Activa una pregunta.
     */
    @PatchMapping("/admin/questions/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Activar pregunta",
        description = "Marca una pregunta como activa."
    )
    public QuestionResponseDto activate(
        @PathVariable Long id
    ) {
        return questionService.activate(id);
    }

    /**
     * Desactiva una pregunta.
     */
    @PatchMapping("/admin/questions/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Desactivar pregunta",
        description = "Marca una pregunta como desactivada."
    )
    public QuestionResponseDto disable(
        @PathVariable Long id
    ) {
        return questionService.disable(id);
    }

    /**
     * Archiva una pregunta.
     */
    @PatchMapping("/admin/questions/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Archivar pregunta",
        description = "Marca una pregunta como archivada."
    )
    public QuestionResponseDto archive(
        @PathVariable Long id
    ) {
        return questionService.archive(id);
    }
}
