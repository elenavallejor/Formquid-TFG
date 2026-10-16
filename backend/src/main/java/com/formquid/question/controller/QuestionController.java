package com.formquid.question.controller;

import com.formquid.common.Category;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.dto.FixedQuestionSummaryDto;
import com.formquid.question.dto.QuestionResponseDto;
import com.formquid.question.service.QuestionService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Expone las operaciones de consulta y administración de preguntas fijas del catálogo.
 */
@RestController
@RequestMapping("/api/v1/catalog/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public PageResponseDto<FixedQuestionSummaryDto> findFixedQuestions(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "false") boolean mine,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "TITLE,DESCRIPTION,CREATOR") Set<CatalogSearchField> searchFields,
        @RequestParam(defaultValue = "NEWEST") CatalogSortOrder sortOrder,
        @RequestParam(required = false) Category category
    ) {
        return questionService.findFixedQuestions(page, size, user, mine, search, searchFields, sortOrder, category);
    }

    @GetMapping("/builder")
    public PageResponseDto<QuestionResponseDto> findFixedQuestionsForBuilder(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Category category
    ) {
        return questionService.findFixedQuestionsForBuilder(page, size, category);
    }

    @GetMapping("/{questionId}")
    public QuestionResponseDto findFixedQuestion(@PathVariable Long questionId) {
        return questionService.findFixedQuestion(questionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public QuestionResponseDto createFixedQuestion(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateFixedQuestionRequestDto request
    ) {
        return questionService.createFixedQuestion(request, user);
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public QuestionResponseDto updateFixedQuestion(
        @PathVariable Long questionId,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateFixedQuestionRequestDto request
    ) {
        return questionService.updateFixedQuestion(questionId, request, user);
    }

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void deleteFixedQuestion(
        @PathVariable Long questionId,
        @AuthenticationPrincipal User user
    ) {
        questionService.deleteFixedQuestion(questionId, user);
    }
}
