package com.formquid.auth.service;

import com.formquid.auth.dto.LoginRequestDto;
import com.formquid.auth.security.JwtService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_normalizesEmailAndReturnsAuthenticatedUser() {
        User user = User.builder()
            .id(7L)
            .email("editor@example.com")
            .name("Editora")
            .role(UserRole.EDITOR)
            .build();
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var result = authService.login(new LoginRequestDto("  EDITOR@EXAMPLE.COM ", "secret123"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> credentials =
            ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(credentials.capture());
        assertThat(credentials.getValue().getPrincipal()).isEqualTo("editor@example.com");
        assertThat(credentials.getValue().getCredentials()).isEqualTo("secret123");
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().id()).isEqualTo(7L);
        assertThat(result.user().role()).isEqualTo(UserRole.EDITOR);
    }
}
