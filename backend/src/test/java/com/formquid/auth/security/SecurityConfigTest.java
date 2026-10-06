package com.formquid.auth.security;

import com.formquid.config.SecurityConfig;
import com.formquid.user.entity.User;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void userDetailsService_normalizesEmailBeforeLookup() {
        UserRepository users = mock(UserRepository.class);
        User user = User.builder().email("person@example.com").build();
        when(users.findByEmail("person@example.com")).thenReturn(Optional.of(user));

        assertThat(config.userDetailsService(users).loadUserByUsername(" PERSON@EXAMPLE.COM "))
            .isSameAs(user);
        verify(users).findByEmail("person@example.com");
    }

    @Test
    void userDetailsService_rejectsUnknownEmail() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> config.userDetailsService(users).loadUserByUsername("missing@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Usuario no encontrado");
    }

    @Test
    void cors_allowsOnlyConfiguredFrontendWithCredentials() {
        var request = new MockHttpServletRequest("GET", "/api/v1/public/surveys/page");
        var configuration = config.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).contains("Content-Type", "X-XSRF-TOKEN");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
