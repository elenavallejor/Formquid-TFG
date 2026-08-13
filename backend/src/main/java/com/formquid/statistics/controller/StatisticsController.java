package com.formquid.statistics.controller;

import com.formquid.statistics.dto.QuestionStatisticsResponseDto;
import com.formquid.statistics.dto.SurveyStatisticsResponseDto;
import com.formquid.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Controlador REST para consultar estadísticas de encuestas.
 *
 * Los endpoints únicamente están disponibles para usuarios autenticados.
 * StatisticsService realiza además la comprobación específica de permisos
 * sobre la encuesta solicitada.
 */
@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN', 'SUPERADMIN')")
@Tag(
    name = "Estadísticas",
    description = "Consulta de resultados y estadísticas agregadas de encuestas"
)
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Devuelve las estadísticas globales de una encuesta.
     *
     * @param surveyId identificador de la encuesta.
     * @param principal usuario autenticado.
     * @return estadísticas generales y desglose de preguntas.
     */
    @GetMapping
    @Operation(
        summary = "Consultar estadísticas de una encuesta",
        description = "Devuelve las estadísticas agregadas de una encuesta si el usuario tiene permisos."
    )
    public SurveyStatisticsResponseDto getSurveyStatistics(
        @PathVariable Long surveyId,
        Principal principal
    ) {
        return statisticsService.getSurveyStatistics(
            surveyId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Devuelve las estadísticas de una pregunta concreta.
     *
     * La pregunta debe pertenecer a la encuesta indicada en la URL.
     *
     * @param surveyId identificador de la encuesta.
     * @param questionId identificador de la pregunta.
     * @param principal usuario autenticado.
     * @return estadísticas de la pregunta.
     */
    @GetMapping("/questions/{questionId}")
    @Operation(
        summary = "Consultar estadísticas de una pregunta",
        description = "Devuelve el desglose estadístico de una pregunta perteneciente a la encuesta."
    )
    public QuestionStatisticsResponseDto getQuestionStatistics(
        @PathVariable Long surveyId,
        @PathVariable Long questionId,
        Principal principal
    ) {
        return statisticsService.getQuestionStatistics(
            surveyId,
            questionId,
            getAuthenticatedEmail(principal)
        );
    }

    /**
     * Obtiene el correo del usuario autenticado almacenado en Principal.
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
