package com.formquid.survey.service;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.exception.ApiException;
import com.formquid.common.dto.PageResponseDto;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.service.NotificationService;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.dto.AdminSurveySummaryDto;
import com.formquid.survey.dto.PublicSurveySummaryDto;
import com.formquid.survey.dto.SurveyDirectorySummaryDto;
import com.formquid.survey.dto.UpdateSurveyRequestDto;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyFontFamily;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.template.service.TemplateService;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCollaboratorRepository collaboratorRepository;
    @Mock
    private CollaborationRequestRepository collaborationRequestRepository;
    @Mock
    private ContentItemRepository contentItemRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private ContentService contentService;
    @Mock
    private TemplateService templateService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SurveyService surveyService;

    @Test
    void mainPageCombinesNameAndTagBeforePagination() {
        PageRequest pageable = PageRequest.of(1, 3);
        when(surveyRepository.findListedOpenPageByTag(
            false,
            "PUBLIC",
            "clima",
            "salud-mental",
            pageable
        )).thenReturn(Page.empty(pageable));

        PageResponseDto<PublicSurveySummaryDto> response = surveyService.findListedOpenPage(
            "  CLIMA  ",
            " #Salud Mental ",
            SurveyAccessMode.PUBLIC,
            1,
            3
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
    }

    @Test
    void administrationLoadsOnlyRequestedSummaryPage() {
        AdminSurveySummaryDto summary = new AdminSurveySummaryDto(
            21L,
            "Encuesta climática",
            "https://example.com/cover.jpg",
            "Editora",
            SurveyLifecycle.OPEN,
            SurveyAccessMode.PUBLIC,
            SurveyVisibility.LISTED,
            18L,
            Instant.parse("2026-09-23T10:00:00Z")
        );
        PageRequest pageable = PageRequest.of(
            1,
            9,
            Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        when(surveyRepository.findAdminSummaries(
            "clima",
            SurveyLifecycle.OPEN,
            SurveyVisibility.LISTED,
            pageable
        )).thenReturn(new PageImpl<>(List.of(summary), pageable, 20));

        PageResponseDto<AdminSurveySummaryDto> response = surveyService.findAdminSummaries(
            "  CLIMA  ",
            SurveyLifecycle.OPEN,
            SurveyVisibility.LISTED,
            1,
            9
        );

        assertThat(response.content()).containsExactly(summary);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(9);
        assertThat(response.totalElements()).isEqualTo(20);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void directoryIncludesDraftsAndPendingCollaborationState() {
        User owner = User.builder().id(10L).name("Editora propietaria").role(UserRole.EDITOR).build();
        User editor = User.builder().id(20L).name("Editor solicitante").role(UserRole.EDITOR).build();
        Survey draft = Survey.builder()
            .id(30L)
            .owner(owner)
            .title("Encuesta en creación")
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .tags(new String[] { "educacion" })
            .updatedAt(Instant.parse("2026-09-23T10:00:00Z"))
            .build();
        PageRequest pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.DESC, "updatedAt"));

        when(surveyRepository.findDirectory("creación", SurveyLifecycle.DRAFT, pageable))
            .thenReturn(new PageImpl<>(List.of(draft), pageable, 1));
        when(collaboratorRepository.findSurveyIdsByUserIdAndSurveyIds(20L, List.of(30L)))
            .thenReturn(List.of());
        when(collaborationRequestRepository.findSurveyIdsByUserIdAndSurveyIds(20L, List.of(30L)))
            .thenReturn(List.of(30L));

        PageResponseDto<SurveyDirectorySummaryDto> response = surveyService.findDirectory(
            editor,
            "  CREACIÓN  ",
            SurveyLifecycle.DRAFT,
            pageable
        );

        assertThat(response.content()).singleElement().satisfies(summary -> {
            assertThat(summary.lifecycleStatus()).isEqualTo(SurveyLifecycle.DRAFT);
            assertThat(summary.editorName()).isEqualTo("Editora propietaria");
            assertThat(summary.tags()).containsExactly("educacion");
            assertThat(summary.ownedByCurrentUser()).isFalse();
            assertThat(summary.collaborating()).isFalse();
            assertThat(summary.collaborationRequestPending()).isTrue();
        });
    }

    @Test
    void surveyUsesTimesNewRomanByDefault() {
        Survey survey = Survey.builder().build();

        survey.normalize();

        assertThat(survey.getFontFamily()).isEqualTo(SurveyFontFamily.TIMES_NEW_ROMAN);
        assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNLISTED);
    }

    @Test
    void openAfterPublicationApprovalGeneratesPublicCode() {
        Survey survey = Survey.builder()
            .id(1L)
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .build();
        List<ContentItem> content = List.of(ContentItem.builder()
            .id(10L)
            .itemType(ContentItemType.QUESTION)
            .position(1)
            .required(false)
            .build());

        when(contentItemRepository.findBySurveyIdOrderByPositionAsc(1L)).thenReturn(content);
        when(contentService.hasQuestion(content)).thenReturn(true);
        when(surveyRepository.existsByPublicCode(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        surveyService.openAfterPublicationApproval(survey);

        assertThat(survey.getLifecycleStatus()).isEqualTo(SurveyLifecycle.OPEN);
        assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.LISTED);
        assertThat(survey.getPublicCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ2-9]{12}");
    }

    @Test
    void administrationCannotCloseDraftSurvey() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User admin = User.builder().id(3L).role(UserRole.ADMIN).build();
        Survey survey = Survey.builder()
            .id(13L)
            .owner(owner)
            .title("Borrador")
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .visibility(SurveyVisibility.UNLISTED)
            .build();

        when(surveyRepository.findById(13L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyService.adminChangeLifecycle(13L, SurveyLifecycle.CLOSED, admin))
            .hasMessage("Una encuesta en borrador debe publicarse antes de poder cerrarse");
        assertThat(survey.getLifecycleStatus()).isEqualTo(SurveyLifecycle.DRAFT);
    }

    @Test
    void editingCollaboratorCannotChangeAccessOrVisibility() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User collaborator = User.builder().id(2L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder()
            .id(10L)
            .owner(owner)
            .title("Encuesta")
            .fontFamily(SurveyFontFamily.ARIAL)
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build();
        UpdateSurveyRequestDto request = new UpdateSurveyRequestDto(
            "Título actualizado",
            "Descripción",
            null,
            SurveyFontFamily.LEXEND,
            SurveyAccessMode.AUTHENTICATED,
            SurveyVisibility.UNLISTED,
            null,
            List.of("colegio")
        );

        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findPermission(10L, 2L)).thenReturn(Optional.of(CollaborationPermission.EDIT));
        when(contentService.findSurveyContent(10L)).thenReturn(List.of());

        surveyService.update(10L, request, collaborator);

        assertThat(survey.getTitle()).isEqualTo("Título actualizado");
        assertThat(survey.getFontFamily()).isEqualTo(SurveyFontFamily.LEXEND);
        assertThat(survey.getAccessMode()).isEqualTo(SurveyAccessMode.PUBLIC);
        assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.LISTED);
    }

    @Test
    void administrationCanHideSurveyWithoutChangingItsLifecycle() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User admin = User.builder().id(3L).role(UserRole.ADMIN).build();
        Survey survey = Survey.builder()
            .id(11L)
            .owner(owner)
            .title("Encuesta supervisada")
            .fontFamily(SurveyFontFamily.ARIAL)
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build();

        when(surveyRepository.findById(11L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findByIdSurveyId(11L)).thenReturn(List.of());
        when(contentService.findSurveyContent(11L)).thenReturn(List.of());

        surveyService.adminChangeVisibility(11L, SurveyVisibility.UNLISTED, admin);

        assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNLISTED);
        assertThat(survey.getLifecycleStatus()).isEqualTo(SurveyLifecycle.OPEN);
    }

    @Test
    void viewingCollaboratorCanPreviewWithoutEditing() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User collaborator = User.builder().id(4L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).build();

        when(collaboratorRepository.findPermission(12L, 4L)).thenReturn(Optional.of(CollaborationPermission.VIEW));

        assertThat(surveyService.canPreviewSurvey(survey, collaborator)).isTrue();
        assertThat(surveyService.canEditSurvey(survey, collaborator)).isFalse();
    }

    @Test
    void passwordProtectedSurveyDoesNotExposeContentBeforeValidation() {
        Survey survey = passwordProtectedSurvey();
        when(surveyRepository.findByPublicCode("ABC123")).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyService.getPublicSurvey("abc123"))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.FORBIDDEN))
            .hasMessage("Esta encuesta requiere contraseña");
    }

    @Test
    void passwordProtectedSurveyExposesContentAfterSuccessfulValidation() {
        Survey survey = passwordProtectedSurvey();
        when(surveyRepository.findByPublicCode("ABC123")).thenReturn(Optional.of(survey));
        when(passwordEncoder.matches("clave-correcta", "hash-seguro")).thenReturn(true);
        when(contentService.findSurveyContent(40L)).thenReturn(List.of());

        var response = surveyService.getPasswordProtectedPublicSurvey("abc123", "clave-correcta");

        assertThat(response.publicCode()).isEqualTo("ABC123");
        assertThat(response.accessMode()).isEqualTo(SurveyAccessMode.PASSWORD_PROTECTED);
        assertThat(response.contentItems()).isEmpty();
    }

    @Test
    void passwordProtectedSurveyRejectsAnInvalidPassword() {
        Survey survey = passwordProtectedSurvey();
        when(surveyRepository.findByPublicCode("ABC123")).thenReturn(Optional.of(survey));
        when(passwordEncoder.matches("clave-incorrecta", "hash-seguro")).thenReturn(false);

        assertThatThrownBy(() -> surveyService.getPasswordProtectedPublicSurvey("abc123", "clave-incorrecta"))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.FORBIDDEN))
            .hasMessage("La contraseña no es correcta");
    }

    private Survey passwordProtectedSurvey() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        return Survey.builder()
            .id(40L)
            .owner(owner)
            .publicCode("ABC123")
            .title("Encuesta protegida")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PASSWORD_PROTECTED)
            .visibility(SurveyVisibility.LISTED)
            .accessPasswordHash("hash-seguro")
            .tags(new String[0])
            .build();
    }
}
