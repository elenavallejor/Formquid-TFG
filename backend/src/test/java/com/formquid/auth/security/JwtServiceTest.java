package com.formquid.auth.security;

import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "a-secure-test-key-with-at-least-thirty-two-bytes";

    @Mock private UserRepository userRepository;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(userRepository, SECRET, 60_000L);
    }

    @Test
    void generateAndExtract_loadsCurrentAuthenticatedPrincipal() {
        User user = User.builder()
            .id(17L)
            .email("editor@example.com")
            .role(UserRole.EDITOR)
            .passwordHash("stored-hash")
            .build();

        when(userRepository.findById(17L)).thenReturn(Optional.of(user));
        User principal = jwtService.extractAuthenticatedUser(jwtService.generateToken(user));

        assertThat(principal).isSameAs(user);
    }

    @Test
    void extractAuthenticatedUser_usesRoleFromDatabaseAfterDemotion() {
        User original = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.SUPERADMIN).passwordHash("stored-hash").build();
        String token = jwtService.generateToken(original);
        User current = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        when(userRepository.findById(17L)).thenReturn(Optional.of(current));

        assertThat(jwtService.extractAuthenticatedUser(token).getRole()).isEqualTo(UserRole.EDITOR);
    }

    @Test
    void extractAuthenticatedUser_rejectsDeletedUser() {
        User user = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        when(userRepository.findById(17L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(jwtService.generateToken(user)))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void extractAuthenticatedUser_rejectsOldEmail() {
        User original = User.builder().id(17L).email("old@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        User current = User.builder().id(17L).email("new@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        when(userRepository.findById(17L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(jwtService.generateToken(original)))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void extractAuthenticatedUser_preservesDatabaseFailure() {
        User user = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        when(userRepository.findById(17L))
            .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(jwtService.generateToken(user)))
            .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void extractAuthenticatedUser_rejectsTokenSignedWithAnotherKey() {
        String token = new JwtService(userRepository, "another-secure-key-with-at-least-thirty-two-bytes", 60_000L)
            .generateToken(User.builder().id(1L).email("user@example.com")
                .role(UserRole.ADMIN).passwordHash("stored-hash").build());

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(token))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_rejectsMissingOrWeakSecret() {
        assertThatThrownBy(() -> new JwtService(userRepository, " ", 60_000L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(userRepository, "short-secret", 60_000L))
            .isInstanceOf(WeakKeyException.class);
        assertThatThrownBy(() -> new JwtService(userRepository,
            "FormQuidSecretKeyForJwtAuthenticationMustBeAtLeastThirtyTwoChars", 60_000L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractAuthenticatedUser_rejectsTokenAfterPasswordChange() {
        User user = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.EDITOR).passwordHash("first-hash").build();
        String previousToken = jwtService.generateToken(user);
        user.setPasswordHash("second-hash");
        when(userRepository.findById(17L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(previousToken))
            .isInstanceOf(JwtException.class);
        assertThat(jwtService.extractAuthenticatedUser(jwtService.generateToken(user)))
            .isSameAs(user);
    }

    @Test
    void extractAuthenticatedUser_keepsTokenAfterProfileChange() {
        User user = User.builder().id(17L).email("editor@example.com")
            .name("Nombre anterior").role(UserRole.EDITOR).passwordHash("stored-hash").build();
        String token = jwtService.generateToken(user);
        user.setName("Nombre nuevo");
        when(userRepository.findById(17L)).thenReturn(Optional.of(user));

        assertThat(jwtService.extractAuthenticatedUser(token)).isSameAs(user);
    }

    @Test
    void extractAuthenticatedUser_rejectsTokenWithoutFingerprint() {
        String legacyToken = Jwts.builder()
            .subject("editor@example.com")
            .claim("userId", "17")
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(legacyToken))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractAuthenticatedUser_rejectsMalformedFingerprint() {
        String malformedToken = Jwts.builder()
            .subject("editor@example.com")
            .claim("userId", "17")
            .claim("credentialsFingerprint", "invalid@fingerprint")
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        User user = User.builder().id(17L).email("editor@example.com")
            .role(UserRole.EDITOR).passwordHash("stored-hash").build();
        when(userRepository.findById(17L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> jwtService.extractAuthenticatedUser(malformedToken))
            .isInstanceOf(JwtException.class);
    }
}
