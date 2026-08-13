package com.formquid.survey.controller;

import com.formquid.survey.dto.AccessSurveyRequestDto;
import com.formquid.survey.dto.PublicSurveyResponseDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.service.PublicSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador destinado al acceso de participantes.
 *
 * Sus endpoints pueden utilizarse sin disponer de una cuenta de usuario.
 */
@RestController
@RequestMapping("/api/v1/public/surveys")
@RequiredArgsConstructor
@Tag(
    name = "Encuestas públicas",
    description = "Acceso de participantes a encuestas públicas y privadas"
)
public class PublicSurveyController {

    private final PublicSurveyService publicSurveyService;

    /**
     * Lista las encuestas públicas disponibles.
     */
    @GetMapping
    @Operation(
        summary = "Listar encuestas públicas",
        description = "Devuelve las encuestas PUBLIC visibles en la página de inicio."
    )
    public List<PublicSurveySummaryDto> findPublic() {
        return publicSurveyService.findPublic();
    }

    /**
     * Obtiene directamente una encuesta PUBLIC.
     */
    @GetMapping("/{publicCode}")
    @Operation(
        summary = "Obtener encuesta pública",
        description = "Devuelve la estructura completa de una encuesta PUBLIC."
    )
    public PublicSurveyResponseDto findPublicByCode(
        @PathVariable String publicCode
    ) {
        return publicSurveyService.findPublicByCode(publicCode);
    }

    /**
     * Accede mediante código a una encuesta PUBLIC o PRIVATE.
     *
     * Las encuestas privadas requieren enviar accessPassword.
     */
    @PostMapping("/{publicCode}/access")
    @Operation(
        summary = "Acceder a encuesta",
        description = "Permite acceder a una encuesta pública o privada mediante su código."
    )
    public PublicSurveyResponseDto accessByCode(
        @PathVariable String publicCode,
        @Valid @RequestBody(required = false) AccessSurveyRequestDto request
    ) {
        return publicSurveyService.accessByCode(
            publicCode,
            request
        );
    }
}
