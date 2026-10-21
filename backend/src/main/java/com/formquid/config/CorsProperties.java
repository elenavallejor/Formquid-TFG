package com.formquid.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    @NotEmpty List<@Pattern(regexp = "^https?://[^*/?#]+$", message = "debe ser un origen HTTP(S) exacto") String> allowedOrigins
) {
}
