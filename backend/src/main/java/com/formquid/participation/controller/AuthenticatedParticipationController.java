package com.formquid.participation.controller;

import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.service.ParticipationService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestiona el envío autenticado de participaciones en encuestas.
 */
@RestController
@RequestMapping("/api/v1/surveys/{publicCode}/participations")
@RequiredArgsConstructor
public class AuthenticatedParticipationController {

    private final ParticipationService participationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationResponseDto submit(@PathVariable String publicCode, @AuthenticationPrincipal User user, @Valid @RequestBody SubmitParticipationRequestDto request) {
        return participationService.submit(publicCode, request, user);
    }
}