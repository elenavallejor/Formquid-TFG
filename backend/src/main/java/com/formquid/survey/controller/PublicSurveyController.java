package com.formquid.survey.controller;

import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyResponseDto;
import com.formquid.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/surveys")
@RequiredArgsConstructor
public class PublicSurveyController {

    private final SurveyService surveyService;

    @GetMapping
    public List<PublicSurveySummaryDto> findListedOpen(@RequestParam(required = false) List<String> tags) {
        return surveyService.findListedOpen(tags);
    }

    @GetMapping("/{surveyId}")
    public SurveyResponseDto get(@PathVariable String surveyId) {
        return surveyService.getPublicSurvey(surveyId);
    }
}
