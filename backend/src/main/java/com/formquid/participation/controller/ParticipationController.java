package com.formquid.participation.controller;

import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.SubmitParticipationRequestDto;
import com.formquid.participation.service.AnonymousRespondentService;
import com.formquid.participation.service.ParticipationService;
import com.formquid.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestiona el envío público de participaciones en encuestas.
 */
@RestController
@RequestMapping("/api/v1/public/surveys/{publicCode}/participations")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;
    private final AnonymousRespondentService anonymousRespondentService;

    @PostMapping
    public ResponseEntity<ParticipationResponseDto> submit(
        @PathVariable String publicCode,
        @AuthenticationPrincipal User user,
        @CookieValue(name = AnonymousRespondentService.COOKIE_NAME, required = false) String respondentToken,
        @Valid @RequestBody SubmitParticipationRequestDto request
    ) {
        if (user != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(participationService.submit(publicCode, request, user, null));
        }
        var identity = anonymousRespondentService.resolve(respondentToken, publicCode);
        var response = participationService.submit(publicCode, request, null, identity.tokenHash());
        var responseBuilder = ResponseEntity.status(HttpStatus.CREATED);
        if (identity.newCookie() != null) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, identity.newCookie().toString());
        }
        return responseBuilder.body(response);
    }
}
