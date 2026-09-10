package com.formquid.content.controller;

import com.formquid.common.exception.ApiException;
import com.formquid.content.dto.AddBlockItemRequestDto;
import com.formquid.content.dto.AddFixedQuestionItemRequestDto;
import com.formquid.content.dto.ContentItemResponseDto;
import com.formquid.content.dto.CreateQuestionItemRequestDto;
import com.formquid.content.dto.CreateTextItemRequestDto;
import com.formquid.content.dto.ReorderContentRequestDto;
import com.formquid.content.dto.UpdateQuestionItemRequestDto;
import com.formquid.content.dto.UpdateRequiredRequestDto;
import com.formquid.content.dto.UpdateTextItemRequestDto;
import com.formquid.content.service.ContentService;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/surveys/{surveyId}/content")
@RequiredArgsConstructor
public class ContentController {

    private final SurveyService surveyService;
    private final ContentService contentService;

    @PostMapping("/texts")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentItemResponseDto addText(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateTextItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.addTextToSurvey(survey, request);
    }

    @PostMapping("/questions/local")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentItemResponseDto addLocalQuestion(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateQuestionItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.addLocalQuestionToSurvey(survey, request, user);
    }

    @PostMapping("/questions/fixed")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentItemResponseDto addFixedQuestion(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody AddFixedQuestionItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.addFixedQuestionToSurvey(survey, request, user);
    }

    @PostMapping("/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentItemResponseDto addBlock(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody AddBlockItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.addBlockToSurvey(survey, request, user);
    }

    @PutMapping("/texts/{itemId}")
    public ContentItemResponseDto updateText(
        @PathVariable Long surveyId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateTextItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.updateSurveyText(survey, itemId, request);
    }

    @PutMapping("/questions/{itemId}")
    public ContentItemResponseDto updateQuestion(
        @PathVariable Long surveyId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateQuestionItemRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.updateSurveyQuestion(survey, itemId, request);
    }

    @PatchMapping("/questions/{itemId}/required")
    public ContentItemResponseDto updateRequired(
        @PathVariable Long surveyId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpdateRequiredRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.updateSurveyQuestionRequired(survey, itemId, request);
    }

    @PatchMapping("/reorder")
    public List<ContentItemResponseDto> reorder(
        @PathVariable Long surveyId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ReorderContentRequestDto request
    ) {
        Survey survey = editableSurvey(surveyId, user);
        return contentService.reorderSurveyContent(survey, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
        @PathVariable Long surveyId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user
    ) {
        Survey survey = editableSurvey(surveyId, user);
        contentService.deleteSurveyItem(survey, itemId);
    }

    private Survey editableSurvey(Long surveyId, User user) {
        Survey survey = surveyService.getSurvey(surveyId);
        if (!surveyService.canEditSurvey(survey, user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes editar esta encuesta");
        }
        surveyService.ensureStructureEditable(survey);
        return survey;
    }
}