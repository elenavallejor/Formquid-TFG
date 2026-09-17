package com.formquid.survey.service;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.content.service.ContentService;
import com.formquid.notification.service.NotificationService;
import com.formquid.participation.repository.ParticipationRepository;
import com.formquid.survey.entity.Survey;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyCollaboratorRepository collaboratorRepository;
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
    void surveyUsesTimesNewRomanByDefault() {
        Survey survey = Survey.builder().build();

        survey.normalize();

        assertThat(survey.getFontFamily()).isEqualTo(SurveyFontFamily.TIMES_NEW_ROMAN);
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
        assertThat(survey.getPublicCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ2-9]{12}");
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
        when(collaboratorRepository.existsByIdSurveyIdAndIdUserIdAndPermission(10L, 2L, CollaborationPermission.EDIT)).thenReturn(true);
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

        when(collaboratorRepository.existsByIdSurveyIdAndIdUserId(12L, 4L)).thenReturn(true);
        when(collaboratorRepository.existsByIdSurveyIdAndIdUserIdAndPermission(12L, 4L, CollaborationPermission.EDIT)).thenReturn(false);

        assertThat(surveyService.canPreviewSurvey(survey, collaborator)).isTrue();
        assertThat(surveyService.canEditSurvey(survey, collaborator)).isFalse();
    }
}
