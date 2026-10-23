package com.formquid.auth.service;

import com.formquid.auth.dto.PasswordResetConfirmDto;
import com.formquid.auth.entity.PasswordResetToken;
import com.formquid.auth.repository.PasswordResetTokenRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.service.EmailNormalizer;
import com.formquid.notification.service.EmailRequested;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Duration REQUEST_COOLDOWN = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Value("${app.password-reset.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public void request(String email) {
        users.findByEmail(EmailNormalizer.normalize(email)).ifPresent(found -> {
            User user = users.findByIdForUpdate(found.getId()).orElseThrow();
            Instant now = Instant.now();
            PasswordResetToken reset = tokens.findById(user.getId()).orElseGet(() -> {
                PasswordResetToken created = new PasswordResetToken();
                created.setUserId(user.getId());
                return created;
            });
            if (reset.getCreatedAt() != null && reset.getCreatedAt().plus(REQUEST_COOLDOWN).isAfter(now)) {
                return;
            }

            byte[] randomBytes = new byte[32];
            RANDOM.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            reset.setTokenHash(hash(rawToken));
            reset.setCreatedAt(now);
            reset.setExpiresAt(now.plus(TOKEN_LIFETIME));
            tokens.save(reset);

            String link = frontendBaseUrl.replaceAll("/+$", "") + "/restablecer-contrasena?token=" + rawToken;
            events.publishEvent(EmailRequested.passwordReset(user.getEmail(), "Restablece tu contraseña de FormQuid",
                "Hemos recibido una solicitud para restablecer tu contraseña.\n\n"
                    + "Abre este enlace para elegir una nueva contraseña:\n" + link + "\n\n"
                    + "El enlace caduca en 30 minutos y solo puede usarse una vez. "
                    + "Si no has solicitado este cambio, ignora este correo.", link));
        });
    }

    @Transactional
    public void confirm(PasswordResetConfirmDto request) {
        String tokenHash = hash(request.token());
        PasswordResetToken candidate = tokens.findByTokenHash(tokenHash)
            .orElseThrow(PasswordResetService::invalidToken);
        User user = users.findByIdForUpdate(candidate.getUserId())
            .orElseThrow(PasswordResetService::invalidToken);
        PasswordResetToken reset = tokens.findById(candidate.getUserId())
            .orElseThrow(PasswordResetService::invalidToken);
        if (!reset.getTokenHash().equals(tokenHash) || !reset.getExpiresAt().isAfter(Instant.now())) {
            throw invalidToken();
        }
        if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        tokens.delete(reset);
        events.publishEvent(new EmailRequested(user.getEmail(), "Tu contraseña de FormQuid ha cambiado",
            "Se ha restablecido la contraseña de tu cuenta. Si no has sido tú, contacta con la administración."));
    }

    private static ApiException invalidToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "El enlace no es válido o ha caducado. Solicita uno nuevo.");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
