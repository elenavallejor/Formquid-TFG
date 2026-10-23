package com.formquid.auth.security;

import com.formquid.auth.dto.PasswordResetConfirmDto;
import com.formquid.auth.repository.PasswordResetTokenRepository;
import com.formquid.auth.service.PasswordResetService;
import com.formquid.common.exception.ApiException;
import com.formquid.config.TestcontainersConfiguration;
import com.formquid.notification.service.EmailRequested;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@RecordApplicationEvents
class PasswordResetFlowIntegrationTest {
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordResetTokenRepository resetTokens;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private ApplicationEvents events;

    @Test
    void resetToken_expiresAndCannotBeReused() {
        User user = createUser();
        String previousJwt = jwtService.generateToken(user);
        passwordResetService.request(user.getEmail());
        EmailRequested email = resetEmail();
        String rawToken = email.body().split("token=")[1].split("\\s")[0];
        assertThat(email.layout()).isEqualTo(EmailRequested.Layout.PASSWORD_RESET);
        assertThat(email.actionUrl()).endsWith("token=" + rawToken);
        var stored = resetTokens.findById(user.getId()).orElseThrow();
        assertThat(stored.getTokenHash()).doesNotContain(rawToken);
        assertThat(email.body()).contains("30 minutos");

        stored.setExpiresAt(Instant.now().minusSeconds(1));
        resetTokens.saveAndFlush(stored);
        assertThatThrownBy(() -> passwordResetService.confirm(new PasswordResetConfirmDto(rawToken, "new-secret-123")))
            .isInstanceOf(ApiException.class);
        assertThat(passwordEncoder.matches("old-secret-123", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();

        stored.setExpiresAt(Instant.now().plusSeconds(60));
        resetTokens.saveAndFlush(stored);
        passwordResetService.confirm(new PasswordResetConfirmDto(rawToken, "new-secret-123"));
        assertThat(resetTokens.findById(user.getId())).isEmpty();
        assertThat(passwordEncoder.matches("new-secret-123", users.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
        assertThatThrownBy(() -> passwordResetService.confirm(new PasswordResetConfirmDto(rawToken, "another-secret-123")))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(previousJwt))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void request_doesNotDiscloseUnknownAddressesAndLimitsRepeatedMail() {
        passwordResetService.request("missing-" + UUID.randomUUID() + "@example.com");
        assertThat(events.stream(EmailRequested.class)).isEmpty();

        User user = createUser();
        passwordResetService.request(user.getEmail());
        passwordResetService.request(user.getEmail());
        assertThat(events.stream(EmailRequested.class)).hasSize(1);
    }

    private User createUser() {
        return users.save(User.builder()
            .role(UserRole.EDITOR)
            .email("reset-" + UUID.randomUUID() + "@example.com")
            .name("Usuario de prueba")
            .institution("FormQuid")
            .passwordHash(passwordEncoder.encode("old-secret-123"))
            .build());
    }

    private EmailRequested resetEmail() {
        return events.stream(EmailRequested.class)
            .filter(event -> event.subject().equals("Restablece tu contraseña de FormQuid"))
            .findFirst().orElseThrow();
    }
}
