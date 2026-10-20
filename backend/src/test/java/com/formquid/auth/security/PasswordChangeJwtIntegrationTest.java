package com.formquid.auth.security;

import com.formquid.config.TestcontainersConfiguration;
import com.formquid.common.exception.ApiException;
import com.formquid.user.dto.UpdateAccountRequestDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import com.formquid.user.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PasswordChangeJwtIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void updatePassword_invalidatesPreviousJwtAndAcceptsRenewedJwt() throws Exception {
        User user = userRepository.save(User.builder()
            .role(UserRole.EDITOR)
            .email("jwt-rotation-" + UUID.randomUUID() + "@example.com")
            .name("Usuario de prueba")
            .institution("FormQuid")
            .passwordHash(passwordEncoder.encode("current-secret"))
            .build());
        String previousToken = jwtService.generateToken(user);

        var result = userService.updateMyAccount(user.getId(), new UpdateAccountRequestDto(
            null, null, new UpdateAccountRequestDto.PasswordData("new-secret"), "current-secret"
        ));

        assertThat(result.tokenRenewalRequired()).isTrue();
        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(previousToken))
            .isInstanceOf(JwtException.class);
        String renewedToken = jwtService.generateToken(result.user());
        assertThat(jwtService.extractAuthenticatedUser(renewedToken).getId())
            .isEqualTo(user.getId());
        mockMvc.perform(get("/api/v1/users/me").cookie(new Cookie("access_token", previousToken)))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").cookie(new Cookie("access_token", renewedToken)))
            .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("new-secret",
            userRepository.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void updatePassword_waitsForConcurrentChangeAndRejectsOldPassword() throws Exception {
        User user = userRepository.save(User.builder()
            .role(UserRole.EDITOR)
            .email("concurrent-password-" + UUID.randomUUID() + "@example.com")
            .name("Usuario de prueba")
            .institution("FormQuid")
            .passwordHash(passwordEncoder.encode("current-secret"))
            .build());
        CountDownLatch rowLocked = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstChange = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                User lockedUser = userRepository.findByIdForUpdate(user.getId()).orElseThrow();
                lockedUser.setPasswordHash(passwordEncoder.encode("first-new-secret"));
                rowLocked.countDown();
                await(releaseLock);
            }));
            assertThat(rowLocked.await(5, TimeUnit.SECONDS)).isTrue();

            try {
                var secondChange = executor.submit(() -> userService.updateMyAccount(
                    user.getId(), new UpdateAccountRequestDto(
                        null, null, new UpdateAccountRequestDto.PasswordData("second-new-secret"),
                        "current-secret"
                    )
                ));
                assertThatThrownBy(() -> secondChange.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
                releaseLock.countDown();
                firstChange.get(5, TimeUnit.SECONDS);
                assertThatThrownBy(() -> secondChange.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ApiException.class);
            } finally {
                releaseLock.countDown();
            }
        }

        assertThat(passwordEncoder.matches("first-new-secret",
            userRepository.findById(user.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tiempo de espera agotado para liberar la fila");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera interrumpida", exception);
        }
    }
}
