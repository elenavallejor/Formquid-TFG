package com.formquid.survey.controller;

import com.formquid.survey.dto.ChangeSurveyLifecycleRequestDto;
import com.formquid.survey.dto.CreateSurveyRequestDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping
    public List<SurveyResponseDto> findMine(@AuthenticationPrincipal User user) {
        return surveyService.findEditableByUser(user);
    }

    @GetMapping("/registered")
    public List<PublicSurveySummaryDto> findVisibleForRegistered(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) List<String> tags
    ) {
        return surveyService.findListedOpenForRegistered(user, tags);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyResponseDto create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateSurveyRequestDto request
    ) {
        return surveyService.create(request, user);
    }

    @GetMapping("/{surveyId}")
    public SurveyResponseDto get(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user
    ) {
        return surveyService.getSurveyForUser(surveyId, user);
    }

    @PutMapping("/{surveyId}")
    public SurveyResponseDto update(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateSurveyRequestDto request
    ) {
        return surveyService.update(surveyId, request, user);
    }

    @PatchMapping("/{surveyId}/lifecycle")
    public SurveyResponseDto changeLifecycle(
        @PathVariable String surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ChangeSurveyLifecycleRequestDto request
    ) {
        return surveyService.changeLifecycle(surveyId, request.lifecycleStatus(), user);
    }

    @DeleteMapping("/{surveyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String surveyId, @AuthenticationPrincipal User user) {
        surveyService.delete(surveyId, user);
    }
}
