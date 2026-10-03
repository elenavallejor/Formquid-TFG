package com.formquid.auth.security;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "a-secure-test-key-with-at-least-thirty-two-bytes";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = serviceWithSecret(SECRET);
    }

    @Test
    void generateAndExtract_preservesMinimalAuthenticatedPrincipal() {
        User user = User.builder()
            .id(17L)
            .email("editor@example.com")
            .role(UserRole.EDITOR)
            .build();

        User principal = jwtService.extractAuthenticatedUser(jwtService.generateToken(user));

        assertThat(principal.getId()).isEqualTo(17L);
        assertThat(principal.getEmail()).isEqualTo("editor@example.com");
        assertThat(principal.getRole()).isEqualTo(UserRole.EDITOR);
        assertThat(principal.getPassword()).isNull();
    }

    @Test
    void extractAuthenticatedUser_rejectsTokenSignedWithAnotherKey() {
        String token = serviceWithSecret("another-secure-key-with-at-least-thirty-two-bytes")
            .generateToken(User.builder().id(1L).email("user@example.com").role(UserRole.ADMIN).build());

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(token))
            .isInstanceOf(JwtException.class);
    }

    private JwtService serviceWithSecret(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", secret);
        ReflectionTestUtils.setField(service, "jwtExpirationMs", 60_000L);
        return service;
    }
}
