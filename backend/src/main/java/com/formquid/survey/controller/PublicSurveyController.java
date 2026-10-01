package com.formquid.survey.controller;

import com.formquid.survey.dto.PublicSurveyAccessRequestDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la consulta pública de encuestas disponibles sin requerir autenticación.
 */
@RestController
@RequestMapping("/api/v1/public/surveys")
@RequiredArgsConstructor
public class PublicSurveyController {

    private final SurveyService surveyService;

    @GetMapping
    public List<PublicSurveySummaryDto> findListedOpen(@RequestParam(required = false) List<String> tags) {
        return surveyService.findListedOpen(tags);
    }

    @GetMapping("/{publicCode}")
    public SurveyResponseDto get(@PathVariable String publicCode) {
        return surveyService.getPublicSurvey(publicCode);
    }

    @PostMapping("/{publicCode}/access")
    public SurveyResponseDto access(
        @PathVariable String publicCode,
        @Valid @RequestBody PublicSurveyAccessRequestDto request
    ) {
        return surveyService.getPasswordProtectedPublicSurvey(publicCode, request.accessPassword());
    }
}
