package com.formquid.survey.service;

import com.formquid.collaboration.entity.CollaborationPermission;
import com.formquid.collaboration.repository.CollaborationRequestRepository;
import com.formquid.collaboration.repository.SurveyCollaboratorRepository;
import com.formquid.common.Category;
import com.formquid.common.service.CategoryCatalogService;
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
import com.formquid.survey.dto.CreateSurveyRequestDto;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock
    private CategoryCatalogService categoryCatalogService;
    @Mock
    private SurveyCoverStorage surveyCoverStorage;

    @InjectMocks
    private SurveyService surveyService;

    @ParameterizedTest
    @EnumSource(SurveyLifecycle.class)
    void delete_whenActorIsOwner_allowsAnyLifecycle(SurveyLifecycle lifecycle) {
        User owner = User.builder().id(7L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).lifecycleStatus(lifecycle).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        surveyService.delete(12L, owner);

        verify(participationRepository).deleteBySurveyId(12L);
        verify(surveyRepository).deleteSurveyById(12L);
        verify(surveyCoverStorage).deleteAfterCommit(12L, survey.getImageKey());
    }

    @Test
    void delete_whenActorIsNotOwner_rejectsDeletion() {
        User owner = User.builder().id(7L).role(UserRole.EDITOR).build();
        User collaborator = User.builder().id(8L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).lifecycleStatus(SurveyLifecycle.CLOSED).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyService.delete(12L, collaborator))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(participationRepository, never()).deleteBySurveyId(12L);
        verify(surveyRepository, never()).deleteSurveyById(12L);
    }

    @Test
    void getCover_whenSurveyIsDraft_requiresSurveyAccess() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        String filename = "123e4567-e89b-12d3-a456-426614174000.png";
        Survey survey = Survey.builder().id(12L).owner(owner)
            .imageKey("/api/v1/public/surveys/12/cover/" + filename)
            .lifecycleStatus(SurveyLifecycle.DRAFT).accessMode(SurveyAccessMode.PUBLIC).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyService.getCover(12L, filename, null))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(surveyCoverStorage, never()).read(filename);

        surveyService.getCover(12L, filename, owner);
        verify(surveyCoverStorage).read(filename);
    }

    @Test
    void getCover_whenSurveyIsOpenAndPublic_allowsAnonymousReader() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        String filename = "123e4567-e89b-12d3-a456-426614174000.jpg";
        Survey survey = Survey.builder().id(12L).owner(owner)
            .imageKey("/api/v1/public/surveys/12/cover/" + filename)
            .lifecycleStatus(SurveyLifecycle.OPEN).accessMode(SurveyAccessMode.PUBLIC).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        surveyService.getCover(12L, filename, null);
        verify(surveyCoverStorage).read(filename);
    }

    @Test
    void getCover_whenSurveyIsPasswordProtected_deniesAnonymousReader() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        String filename = "123e4567-e89b-12d3-a456-426614174000.jpg";
        Survey survey = Survey.builder().id(12L).owner(owner)
            .imageKey("/api/v1/public/surveys/12/cover/" + filename)
            .lifecycleStatus(SurveyLifecycle.OPEN).accessMode(SurveyAccessMode.PASSWORD_PROTECTED).build();
        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyService.getCover(12L, filename, null))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(surveyCoverStorage, never()).read(filename);
    }

    @Test
    void getPasswordProtectedPublicSurvey_afterValidPassword_inlinesLocalCover() {
        Survey survey = passwordProtectedSurvey();
        String imageKey = "/api/v1/public/surveys/40/cover/123e4567-e89b-12d3-a456-426614174000.png";
        survey.setImageKey(imageKey);
        when(surveyRepository.findByPublicCode("ABC123")).thenReturn(Optional.of(survey));
        when(passwordEncoder.matches("clave-correcta", "hash-seguro")).thenReturn(true);
        when(surveyCoverStorage.inlineForAuthorizedRespondent(40L, imageKey))
            .thenReturn("data:image/png;base64,aW1hZ2Vu");
        when(contentService.findSurveyContent(40L)).thenReturn(List.of());

        var response = surveyService.getPasswordProtectedPublicSurvey("abc123", "clave-correcta");

        assertThat(response.imageKey()).isEqualTo("data:image/png;base64,aW1hZ2Vu");
    }

    @Test
    void surveyCannotUsePersonalDataCategory() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        CreateSurveyRequestDto request = new CreateSurveyRequestDto(
            "Encuesta", null, null, null, null, null, null, List.of(), Category.DATOS_PERSONALES
        );

        assertThatThrownBy(() -> surveyService.create(request, owner))
            .isInstanceOf(ApiException.class)
            .hasMessage("Una encuesta no puede ser de datos personales");
        verify(surveyRepository, never()).save(any());
    }

    @Test
    void surveyPersistsTechnologyCategory() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        CreateSurveyRequestDto request = new CreateSurveyRequestDto(
            "Encuesta", null, null, null, null, null, null, List.of(), Category.TELEFONO_INTELIGENTE
        );
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> {
            Survey survey = invocation.getArgument(0);
            survey.setId(11L);
            return survey;
        });
        when(contentService.findSurveyContent(11L)).thenReturn(List.of());

        var response = surveyService.create(request, owner);

        assertThat(response.category()).isEqualTo(Category.TELEFONO_INTELIGENTE);
    }

    @Test
    void mainPageCombinesNameAndTagBeforePagination() {
        PageRequest pageable = PageRequest.of(1, 3);
        when(surveyRepository.findListedOpenPageByTag(
            false,
            "PUBLIC",
            "",
            "clima",
            "salud-mental",
            pageable
        )).thenReturn(Page.empty(pageable));

        PageResponseDto<PublicSurveySummaryDto> response = surveyService.findListedOpenPage(
            "  CLIMA  ",
            " #Salud Mental ",
            SurveyAccessMode.PUBLIC,
            1,
            3,
            null
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
    }

    @Test
    void mainPageUsesStableCreatedAtAndIdOrder() {
        PageRequest pageable = PageRequest.of(
            0,
            9,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        when(surveyRepository.findListedOpenPage(
            SurveyLifecycle.OPEN,
            SurveyVisibility.LISTED,
            false,
            SurveyAccessMode.AUTHENTICATED,
            SurveyAccessMode.PUBLIC,
            null,
            "",
            pageable
        )).thenReturn(Page.empty(pageable));

        PageResponseDto<PublicSurveySummaryDto> response = surveyService.findListedOpenPage(
            null, null, SurveyAccessMode.PUBLIC, 0, 9, null
        );

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(9);
    }

    @Test
    void mainPageRejectsUnboundedPageSize() {
        assertThatThrownBy(() -> surveyService.findListedOpenPage(null, null, null, 0, 51, null))
            .isInstanceOf(ApiException.class)
            .hasMessage("Paginación de encuestas no válida");
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
            Instant.parse("2026-09-23T10:00:00Z"),
            null
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
            List.of("colegio"),
            Category.PORTATIL
        );

        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findPermission(10L, 2L)).thenReturn(Optional.of(CollaborationPermission.EDIT));
        when(contentService.findSurveyContent(10L)).thenReturn(List.of());

        surveyService.update(10L, request, collaborator);

        assertThat(survey.getTitle()).isEqualTo("Título actualizado");
        assertThat(survey.getFontFamily()).isEqualTo(SurveyFontFamily.LEXEND);
        assertThat(survey.getAccessMode()).isEqualTo(SurveyAccessMode.PUBLIC);
        assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.LISTED);
        assertThat(survey.getCategory()).isEqualTo(Category.PORTATIL);
    }

    @Test
    void update_acceptsAndPreservesTenTags() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder()
            .id(10L)
            .owner(owner)
            .title("Encuesta")
            .fontFamily(SurveyFontFamily.ARIAL)
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.UNLISTED)
            .build();
        List<String> tags = IntStream.rangeClosed(1, Survey.MAX_TAGS)
            .mapToObj(index -> "etiqueta-" + index)
            .toList();
        UpdateSurveyRequestDto request = new UpdateSurveyRequestDto(
            "Encuesta",
            null,
            null,
            SurveyFontFamily.ARIAL,
            SurveyAccessMode.PUBLIC,
            SurveyVisibility.UNLISTED,
            null,
            tags,
            null
        );

        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(contentService.findSurveyContent(10L)).thenReturn(List.of());

        surveyService.update(10L, request, owner);

        assertThat(survey.getTags()).containsExactlyElementsOf(tags);
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
    void viewingCollaboratorCannotEdit() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User collaborator = User.builder().id(4L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).build();

        when(collaboratorRepository.findPermission(12L, 4L)).thenReturn(Optional.of(CollaborationPermission.VIEW));

        assertThat(surveyService.canEditSurvey(survey, collaborator)).isFalse();
    }

    @Test
    void resultsAccess_allowsOwnerViewingCollaboratorAndAdministrator() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User collaborator = User.builder().id(2L).role(UserRole.EDITOR).build();
        User administrator = User.builder().id(3L).role(UserRole.ADMIN).build();
        Survey survey = Survey.builder().id(12L).owner(owner).build();

        when(collaboratorRepository.findPermission(12L, 2L))
            .thenReturn(Optional.of(CollaborationPermission.VIEW));
        when(participationRepository.existsBySurveyId(12L)).thenReturn(true);

        assertThatCode(() -> surveyService.ensureCanViewResults(survey, owner))
            .doesNotThrowAnyException();
        assertThatCode(() -> surveyService.ensureCanViewResults(survey, collaborator))
            .doesNotThrowAnyException();
        assertThatCode(() -> surveyService.ensureCanViewResults(survey, administrator))
            .doesNotThrowAnyException();
    }

    @Test
    void resultsAccess_rejectsEditorWithoutOwnershipOrCollaboration() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User unrelatedEditor = User.builder().id(4L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).build();

        when(collaboratorRepository.findPermission(12L, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.ensureCanViewResults(survey, unrelatedEditor))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN))
            .hasMessage("No puedes consultar las estadísticas de esta encuesta");
    }

    @Test
    void resultsAccess_rejectsSurveyWithoutRegisteredResponses() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder().id(12L).owner(owner).build();

        when(participationRepository.existsBySurveyId(12L)).thenReturn(false);

        assertThatThrownBy(() -> surveyService.ensureCanViewResults(survey, owner))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> {
                ApiException apiException = (ApiException) exception;
                assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(apiException.getCode()).isEqualTo("SURVEY_RESULTS_NOT_AVAILABLE");
            })
            .hasMessage("La encuesta todavía no tiene respuestas registradas");
    }

    @Test
    void resultsView_rejectsUnrelatedEditorEvenWhenSurveyIsPublicAndOpen() {
        User owner = User.builder().id(1L).role(UserRole.EDITOR).build();
        User unrelatedEditor = User.builder().id(4L).role(UserRole.EDITOR).build();
        Survey survey = Survey.builder()
            .id(12L)
            .owner(owner)
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build();

        when(surveyRepository.findById(12L)).thenReturn(Optional.of(survey));
        when(collaboratorRepository.findPermission(12L, 4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.getSurveyForResults(12L, unrelatedEditor))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> assertThat(((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN))
            .hasMessage("No puedes consultar las estadísticas de esta encuesta");
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
