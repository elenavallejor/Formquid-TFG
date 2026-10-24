package com.formquid.survey.controller;

import com.formquid.common.Category;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.survey.dto.PublicSurveyAccessRequestDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.entity.SurveyAccessMode;
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

    @GetMapping("/page")
    public PageResponseDto<PublicSurveySummaryDto> findListedOpenPage(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String tag,
        @RequestParam(required = false) SurveyAccessMode accessMode,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "9") int size,
        @RequestParam(name = "category", required = false) List<Category> categories
    ) {
        return surveyService.findListedOpenPage(search, tag, accessMode, page, size, categories);
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
