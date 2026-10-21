package com.formquid.survey.controller;

import com.formquid.survey.service.SurveyCoverStorage;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** La lectura comprueba los permisos de la encuesta antes de abrir el archivo. */
@RestController
@RequestMapping("/api/v1/public/surveys")
@RequiredArgsConstructor
public class PublicSurveyCoverController {
    private final SurveyService surveyService;

    @GetMapping("/{surveyId}/cover/{filename:.+}")
    public ResponseEntity<byte[]> getCover(
        @PathVariable Long surveyId,
        @PathVariable String filename,
        @AuthenticationPrincipal User user
    ) {
        SurveyCoverStorage.StoredCover cover = surveyService.getCover(surveyId, filename, user);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(cover.contentType()))
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.noStore())
            .body(cover.bytes());
    }
}
