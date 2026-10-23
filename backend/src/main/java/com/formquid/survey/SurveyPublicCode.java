package com.formquid.survey;

import com.formquid.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public final class SurveyPublicCode {

    private SurveyPublicCode() {
    }

    public static String normalize(String publicCode) {
        if (publicCode == null || publicCode.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Encuesta no encontrada");
        }
        return publicCode.trim().toUpperCase(Locale.ROOT);
    }
}
