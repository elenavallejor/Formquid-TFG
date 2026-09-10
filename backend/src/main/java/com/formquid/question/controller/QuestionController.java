package com.formquid.question.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return questionService.findFixedQuestions(page, size);
    }

    @GetMapping("/builder")
    public PageResponseDto<QuestionResponseDto> findFixedQuestionsForBuilder(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return questionService.findFixedQuestionsForBuilder(page, size);
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

    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public void deleteFixedQuestion(@PathVariable Long questionId) {
        questionService.deleteFixedQuestion(questionId);
    }
}