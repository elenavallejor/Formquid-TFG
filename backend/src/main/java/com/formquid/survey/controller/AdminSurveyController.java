package com.formquid.survey.controller;

import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.dto.ChangeSurveyLifecycleRequestDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone las operaciones administrativas de supervisión sobre las encuestas de la plataforma.
 */
@RestController
@RequestMapping("/api/v1/admin/surveys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminSurveyController {

    private final SurveyService surveyService;

    @GetMapping
    public List<AdminSurveySummaryDto> findAll() {
        return surveyService.findAdminSummaries();
    }

    @PatchMapping("/{surveyId}/lifecycle")
    public SurveyResponseDto changeLifecycle(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ChangeSurveyLifecycleRequestDto request
    ) {
        return surveyService.adminChangeLifecycle(surveyId, request.lifecycleStatus(), user);
    }

    @DeleteMapping("/{surveyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user
    ) {
        surveyService.adminDelete(surveyId, user);
    }
}
