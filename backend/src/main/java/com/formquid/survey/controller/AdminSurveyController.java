package com.formquid.survey.controller;

import com.formquid.common.dto.CountResponseDto;
import com.formquid.survey.dto.SurveySummaryResponseDto;
import com.formquid.survey.entity.SurveyStatus;
import com.formquid.survey.service.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador administrativo de encuestas.
 *
 * Agrupa los listados y contadores utilizados por administradores
 * y superadministradores.
 */
@RestController
@RequestMapping("/api/v1/admin/surveys")
@RequiredArgsConstructor
@Tag(
    name = "Administración de encuestas",
    description = "Consulta administrativa de encuestas"
)
public class AdminSurveyController {

    private final SurveyService surveyService;

    /**
     * Lista todas las encuestas.
     *
     * Si se proporciona status, filtra por dicho estado.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Listar encuestas",
        description = "Devuelve todas las encuestas, opcionalmente filtradas por estado."
    )
    public List<SurveySummaryResponseDto> findAll(
        @RequestParam(required = false) SurveyStatus status
    ) {
        return surveyService.findAllForAdmin(status);
    }

    /**
     * Devuelve el número total de encuestas o el número
     * correspondiente a un estado concreto.
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    @Operation(
        summary = "Contar encuestas",
        description = "Devuelve el total de encuestas o el total de un estado concreto."
    )
    public CountResponseDto count(
        @RequestParam(required = false) SurveyStatus status
    ) {
        return new CountResponseDto(
            surveyService.count(status)
        );
    }
}
