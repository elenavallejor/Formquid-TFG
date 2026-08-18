package com.formquid.question.controller;

import com.formquid.question.dto.CreateFixedQuestionRequestDto;
import com.formquid.question.dto.QuestionResponseDto;
import com.formquid.question.service.QuestionService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public List<QuestionResponseDto> findFixedQuestions() {
        return questionService.findFixedQuestions();
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
}
