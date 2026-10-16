package com.formquid.controller;

import com.formquid.common.Category;
import com.formquid.auth.controller.AuthController;
import com.formquid.auth.dto.AuthUserResponseDto;
import com.formquid.auth.dto.LoginResult;
import com.formquid.auth.security.JwtAuthenticationFilter;
import com.formquid.auth.security.JwtCookieService;
import com.formquid.auth.security.JwtService;
import com.formquid.auth.service.AuthService;
import com.formquid.config.SecurityConfig;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.common.exception.ApiException;
import com.formquid.participation.controller.ParticipationController;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.service.AnonymousRespondentService;
import com.formquid.participation.service.ParticipationService;
import com.formquid.registration.controller.RegistrationRequestController;
import com.formquid.registration.dto.RegistrationRequestResponseDto;
import com.formquid.registration.service.RegistrationRequestService;
import com.formquid.survey.controller.PublicSurveyController;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.service.SurveyService;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    AuthController.class,
    PublicSurveyController.class,
    ParticipationController.class,
    RegistrationRequestController.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class PublicControllersWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtCookieService jwtCookieService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private SurveyService surveyService;
    @MockitoBean private ParticipationService participationService;
    @MockitoBean private AnonymousRespondentService anonymousRespondentService;
    @MockitoBean private RegistrationRequestService registrationRequestService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void login_returnsUserAndSecureCookie() throws Exception {
        var user = new AuthUserResponseDto(7L, "editor@example.com", "Editora", UserRole.EDITOR);
        when(authService.login(any())).thenReturn(new LoginResult("signed-token", user));
        when(jwtCookieService.createCookie("signed-token"))
            .thenReturn(ResponseCookie.from("access_token", "signed-token").httpOnly(true).build());

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {"email":"editor@example.com","password":"secret123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("access_token=signed-token")))
            .andExpect(jsonPath("$.user.id").value(7))
            .andExpect(jsonPath("$.user.role").value("EDITOR"));
    }

    @Test
    void login_rejectsInvalidBodyBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Error de validación en los campos enviados"))
            .andExpect(jsonPath("$.fields.email").exists())
            .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void publicSurveyList_bindsFiltersAndReturnsPageMetadata() throws Exception {
        when(surveyService.findListedOpenPage("clima", "tecnologia", SurveyAccessMode.PUBLIC, 1, 3, Category.TELEFONO_INTELIGENTE))
            .thenReturn(new PageResponseDto<>(List.of(new PublicSurveySummaryDto(
                "ABC123", "Encuesta", "Descripción", null, SurveyAccessMode.PUBLIC,
                List.of("infancia", "tecnologia"), null
            )), 1, 3, 4, 2));

        mockMvc.perform(get("/api/v1/public/surveys/page")
                .param("search", "clima")
                .param("tag", "tecnologia")
                .param("accessMode", "PUBLIC")
                .param("category", "TELEFONO_INTELIGENTE")
                .param("page", "1")
                .param("size", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].publicCode").value("ABC123"))
            .andExpect(jsonPath("$.content[0].tags.length()").value(2))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(3))
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void anonymousParticipation_setsIdentityCookieAndReturnsCreated() throws Exception {
        var identityCookie = ResponseCookie.from(AnonymousRespondentService.COOKIE_NAME, "token").httpOnly(true).build();
        when(anonymousRespondentService.resolve(null, "ABC123"))
            .thenReturn(new AnonymousRespondentService.Identity("token-hash", identityCookie));
        when(participationService.submit(eq("ABC123"), any(), eq(null), eq("token-hash")))
            .thenReturn(new ParticipationResponseDto(5L, "ABC123", Instant.parse("2026-01-01T10:00:00Z"), List.of()));

        mockMvc.perform(post("/api/v1/public/surveys/ABC123/participations")
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {"answers":[{"questionId":12,"textValue":"Respuesta"}]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("formquid_respondent=token")))
            .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void repeatedParticipation_returnsStableConflictCode() throws Exception {
        when(anonymousRespondentService.resolve(null, "ABC123"))
            .thenReturn(new AnonymousRespondentService.Identity("token-hash", null));
        when(participationService.submit(eq("ABC123"), any(), eq(null), eq("token-hash")))
            .thenThrow(new ApiException(
                HttpStatus.CONFLICT,
                ParticipationService.ALREADY_SUBMITTED_CODE,
                "Ya respondiste esta encuesta. Solo se puede enviar una vez."
            ));

        mockMvc.perform(post("/api/v1/public/surveys/ABC123/participations")
                .with(csrf())
                .contentType("application/json")
                .content("{\"answers\":[{\"questionId\":12,\"textValue\":\"Respuesta\"}]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ParticipationService.ALREADY_SUBMITTED_CODE))
            .andExpect(jsonPath("$.message").value("Ya respondiste esta encuesta. Solo se puede enviar una vez."));
    }

    @Test
    void registrationRequest_acceptsValidPublicRequest() throws Exception {
        when(registrationRequestService.create(any())).thenReturn(new RegistrationRequestResponseDto(
            4L, "person@example.com", "Persona", "Colegio", "España", "Motivación",
            Instant.parse("2026-01-01T10:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/registration-requests")
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {
                      "email":"person@example.com",
                      "name":"Persona",
                      "institution":"Colegio",
                      "country":"España",
                      "motivation":"Motivación",
                      "password":"secret123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(4))
            .andExpect(jsonPath("$.country").value("España"))
            .andExpect(jsonPath("$.gender").doesNotExist());

        verify(registrationRequestService).create(any());
    }

    @Test
    void publicMutation_withoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/registration-requests")
                .contentType("application/json")
                .content("""
                    {
                      "email":"person@example.com",
                      "name":"Persona",
                      "institution":"Colegio",
                      "motivation":"Motivación",
                      "password":"secret123"
                    }
                    """))
            .andExpect(status().isForbidden());
    }
}
