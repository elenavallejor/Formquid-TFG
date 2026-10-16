package com.formquid.controller;

import com.formquid.auth.security.JwtAuthenticationFilter;
import com.formquid.auth.security.JwtService;
import com.formquid.collaboration.controller.CollaborationController;
import com.formquid.collaboration.controller.CollaborationOverviewController;
import com.formquid.collaboration.service.CollaborationService;
import com.formquid.config.SecurityConfig;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.content.controller.ContentController;
import com.formquid.content.service.ContentService;
import com.formquid.participation.controller.AuthenticatedParticipationController;
import com.formquid.participation.controller.ParticipationResultsController;
import com.formquid.participation.dto.ParticipationDetailResponseDto;
import com.formquid.participation.dto.ParticipationResponseDto;
import com.formquid.participation.dto.ParticipationSummaryResponseDto;
import com.formquid.participation.service.ParticipationService;
import com.formquid.statistics.controller.StatisticsController;
import com.formquid.statistics.service.StatisticsService;
import com.formquid.survey.controller.SurveyController;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyResultsSummaryDto;
import com.formquid.survey.dto.SurveyResultsSearchField;
import com.formquid.survey.dto.SurveyResultsSortOrder;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.service.SurveyService;
import com.formquid.survey.service.SurveyOdtImportService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    SurveyController.class,
    ContentController.class,
    CollaborationController.class,
    CollaborationOverviewController.class,
    StatisticsController.class,
    AuthenticatedParticipationController.class,
    ParticipationResultsController.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class SurveyFlowControllersWebTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SurveyService surveyService;
    @MockitoBean private SurveyOdtImportService surveyOdtImportService;
    @MockitoBean private ContentService contentService;
    @MockitoBean private CollaborationService collaborationService;
    @MockitoBean private StatisticsService statisticsService;
    @MockitoBean private ParticipationService participationService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void surveyDetails_passAuthenticatedUserToService() throws Exception {
        User editor = editor();

        mockMvc.perform(get("/api/v1/surveys/4").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(surveyService).getSurveyForUser(4L, editor);
    }

    @Test
    void wordImportPreview_passesTheUploadedDocumentToService() throws Exception {
        User editor = editor();

        mockMvc.perform(multipart("/api/v1/surveys/import/preview")
                .file("file", "documento".getBytes())
                .with(authentication(tokenFor(editor)))
                .with(csrf()))
            .andExpect(status().isOk());

        verify(surveyOdtImportService).preview(any());
    }

    @Test
    void importedSurveyCreation_passesTheAuthenticatedEditorToService() throws Exception {
        User editor = editor();

        mockMvc.perform(post("/api/v1/surveys/from-import")
                .contentType("application/json")
                .content("""
                    {
                      "survey": {
                        "title": "Encuesta importada",
                        "accessMode": "PUBLIC",
                        "visibility": "UNLISTED",
                        "tags": []
                      },
                      "content": { "items": [] }
                    }
                    """)
                .with(authentication(tokenFor(editor)))
                .with(csrf()))
            .andExpect(status().isCreated());

        verify(surveyOdtImportService).createImported(any(), same(editor));
    }

    @Test
    void surveyResultsDetails_passAuthenticatedUserToRestrictedService() throws Exception {
        User editor = editor();

        mockMvc.perform(get("/api/v1/surveys/4/results").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(surveyService).getSurveyForResults(4L, editor);
    }

    @Test
    void resultSurveyList_passesFiltersAndPaginationToService() throws Exception {
        User editor = editor();
        when(surveyService.findResultSurveys(
            editor,
            "clima",
            Set.of(SurveyResultsSearchField.TITLE),
            SurveyLifecycle.CLOSED,
            SurveyResultsSortOrder.OLDEST,
            1,
            18
        ))
            .thenReturn(new PageResponseDto<>(List.of(new SurveyResultsSummaryDto(
                4L,
                "Encuesta",
                null,
                "Editora",
                SurveyLifecycle.CLOSED,
                3,
                Instant.parse("2026-10-06T10:00:00Z"), null
            )), 1, 18, 20, 2));

        mockMvc.perform(get("/api/v1/surveys/results")
                .with(authentication(tokenFor(editor)))
                .param("search", "clima")
                .param("searchFields", "TITLE")
                .param("lifecycleStatus", "CLOSED")
                .param("sortOrder", "OLDEST")
                .param("page", "1")
                .param("size", "18"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].responses").value(3))
            .andExpect(jsonPath("$.page").value(1));

        verify(surveyService).findResultSurveys(
            editor,
            "clima",
            Set.of(SurveyResultsSearchField.TITLE),
            SurveyLifecycle.CLOSED,
            SurveyResultsSortOrder.OLDEST,
            1,
            18
        );
    }

    @Test
    void registeredSurveyListPassesPaginationToService() throws Exception {
        User editor = editor();
        when(surveyService.findListedOpenForRegisteredPage(
            editor, "clima", "educacion", SurveyAccessMode.AUTHENTICATED, 2, 9, null
        )).thenReturn(new PageResponseDto<>(List.of(new PublicSurveySummaryDto(
            "ABC123", "Encuesta", null, null, SurveyAccessMode.AUTHENTICATED, List.of("educacion"), null
        )), 2, 9, 20, 3));

        mockMvc.perform(get("/api/v1/surveys/registered/page")
                .with(authentication(tokenFor(editor)))
                .param("search", "clima")
                .param("tag", "educacion")
                .param("accessMode", "AUTHENTICATED")
                .param("page", "2")
                .param("size", "9"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].publicCode").value("ABC123"))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void createSurvey_rejectsMoreThanTenTags() throws Exception {
        User editor = editor();

        mockMvc.perform(post("/api/v1/surveys")
                .with(authentication(tokenFor(editor)))
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {
                      "title": "Encuesta",
                      "tags": ["t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11"]
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(surveyService);
    }

    @Test
    void deleteContent_checksEditPermissionBeforeDelegating() throws Exception {
        User editor = editor();
        Survey survey = Survey.builder().id(4L).owner(editor).build();
        when(surveyService.getSurvey(4L)).thenReturn(survey);
        when(surveyService.canEditSurvey(survey, editor)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/surveys/4/content/19")
                .with(authentication(tokenFor(editor)))
                .with(csrf()))
            .andExpect(status().isNoContent());

        verify(surveyService).ensureStructureEditable(survey);
        verify(contentService).deleteSurveyItem(survey, 19L);
    }

    @Test
    void collaborators_checkResultsAccessBeforeReturningData() throws Exception {
        User editor = editor();
        Survey survey = Survey.builder().id(4L).owner(editor).build();
        when(surveyService.getSurvey(4L)).thenReturn(survey);
        when(collaborationService.findCollaborators(4L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/surveys/4/collaborators")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        verify(surveyService).ensureCanViewManagedSurvey(survey, editor);
    }

    @Test
    void collaborationOverview_usesCurrentUserId() throws Exception {
        User editor = editor();
        when(collaborationService.findRequestsForUser(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/collaboration-requests")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(collaborationService).findRequestsForUser(5L);
    }

    @Test
    void questionStatistics_bindBothPathVariables() throws Exception {
        User editor = editor();

        mockMvc.perform(get("/api/v1/surveys/4/statistics/questions/12")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(statisticsService).getQuestionStatistics(4L, 12L, editor);
    }

    @Test
    void authenticatedParticipation_returnsCreated() throws Exception {
        User editor = editor();
        when(participationService.submit(org.mockito.ArgumentMatchers.eq("ABC123"), any(), same(editor), org.mockito.ArgumentMatchers.isNull()))
            .thenReturn(new ParticipationResponseDto(
                15L, "ABC123", Instant.parse("2026-01-01T10:00:00Z"), List.of()
            ));

        mockMvc.perform(post("/api/v1/surveys/ABC123/participations")
                .with(authentication(tokenFor(editor)))
                .with(csrf())
                .contentType("application/json")
                .content("{\"answers\":[{\"questionId\":12,\"booleanValue\":true}]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void participationResults_passPaginationAndAuthenticatedUserToService() throws Exception {
        User editor = editor();
        Instant submittedAt = Instant.parse("2026-10-06T10:00:00Z");
        when(participationService.findParticipationPage(4L, editor, 2, 25))
            .thenReturn(new PageResponseDto<>(
                List.of(new ParticipationSummaryResponseDto(15L, submittedAt)),
                2,
                25,
                60,
                3
            ));

        mockMvc.perform(get("/api/v1/surveys/4/participations")
                .with(authentication(tokenFor(editor)))
                .param("page", "2")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(15))
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.totalPages").value(3));

        verify(participationService).findParticipationPage(4L, editor, 2, 25);
    }

    @Test
    void participationDetail_passesBothIdentifiersAndAuthenticatedUserToService() throws Exception {
        User editor = editor();
        when(participationService.getParticipationDetail(4L, 15L, editor))
            .thenReturn(new ParticipationDetailResponseDto(
                15L,
                4L,
                "Encuesta",
                Instant.parse("2026-10-06T10:00:00Z"),
                List.of()
            ));

        mockMvc.perform(get("/api/v1/surveys/4/participations/15")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(15))
            .andExpect(jsonPath("$.surveyId").value(4))
            .andExpect(jsonPath("$.answers").isArray());

        verify(participationService).getParticipationDetail(4L, 15L, editor);
    }

    @Test
    void protectedSurveyRoute_withoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/surveys/4"))
            .andExpect(status().isUnauthorized());
    }

    private User editor() {
        return User.builder().id(5L).email("editor@example.com").role(UserRole.EDITOR).build();
    }

    private UsernamePasswordAuthenticationToken tokenFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
