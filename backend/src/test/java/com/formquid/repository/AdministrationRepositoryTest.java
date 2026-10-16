package com.formquid.repository;

import com.formquid.config.PostgresDataJpaTest;
import com.formquid.dashboard.repository.AdminDashboardRepository;
import com.formquid.elementrequest.entity.ElementRequest;
import com.formquid.elementrequest.entity.RequestedElementType;
import com.formquid.elementrequest.repository.ElementRequestRepository;
import com.formquid.notification.entity.Notification;
import com.formquid.notification.entity.NotificationChannel;
import com.formquid.notification.entity.NotificationTargetType;
import com.formquid.notification.entity.NotificationType;
import com.formquid.notification.repository.NotificationRepository;
import com.formquid.participation.entity.Participation;
import com.formquid.registration.entity.RegistrationRequest;
import com.formquid.registration.repository.RegistrationRequestRepository;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.survey.repository.SurveyRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresDataJpaTest
@Import(AdminDashboardRepository.class)
class AdministrationRepositoryTest {

    @Autowired private UserRepository users;
    @Autowired private SurveyRepository surveys;
    @Autowired private NotificationRepository notifications;
    @Autowired private RegistrationRequestRepository registrationRequests;
    @Autowired private ElementRequestRepository elementRequests;
    @Autowired private AdminDashboardRepository dashboard;
    @Autowired private EntityManager entityManager;

    @Test
    void administrationProjections_returnOnlyRequestedDataAndParticipationCount() {
        User owner = persistUser("owner-admin-view@example.com", "Propietaria", UserRole.EDITOR);
        owner.setCountry("España");
        Survey survey = persistSurvey(owner, "ADMINVIEW012", "Encuesta visible");
        persist(Participation.builder().survey(survey).respondentTokenHash("a".repeat(64)).build());
        persist(Participation.builder().survey(survey).respondentTokenHash("b".repeat(64)).build());
        entityManager.flush();

        var userProjection = users.findResponseById(owner.getId()).orElseThrow();
        var surveyPage = surveys.findAdminSummaries(
            "visible", SurveyLifecycle.OPEN, SurveyVisibility.LISTED, PageRequest.of(0, 10)
        );

        assertThat(userProjection.email()).isEqualTo("owner-admin-view@example.com");
        assertThat(userProjection.name()).isEqualTo("Propietaria");
        assertThat(userProjection.country()).isEqualTo("España");
        assertThat(users.findResponses(
            "propietaria",
            true,
            false,
            false,
            false,
            false,
            PageRequest.of(0, 10, Sort.by("name"))
        )).extracting(UserResponseDto::id).contains(owner.getId());
        assertThat(users.findResponses(
            "españa", false, false, false, true, false, PageRequest.of(0, 10)
        )).extracting(UserResponseDto::id).contains(owner.getId());
        assertThat(users.findResponses(
            "editor",
            false,
            false,
            false,
            false,
            true,
            PageRequest.of(0, 10, Sort.by("name"))
        )).extracting(UserResponseDto::id).contains(owner.getId());
        assertThat(surveyPage).singleElement().satisfies(summary -> {
            assertThat(summary.title()).isEqualTo("Encuesta visible");
            assertThat(summary.editorName()).isEqualTo("Propietaria");
            assertThat(summary.responses()).isEqualTo(2);
        });
    }

    @Test
    void notificationQueries_filterUnreadAndMarkAllAtomically() {
        User recipient = persistUser("notifications-repo@example.com", "Receptora", UserRole.EDITOR);
        Notification unread = persistNotification(recipient, "Pendiente");
        Notification read = persistNotification(recipient, "Leída");
        read.markAsRead();
        entityManager.flush();

        var unreadPage = notifications.findResponses(
            recipient.getId(), true, null, PageRequest.of(0, 10)
        );

        assertThat(unreadPage).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(unread.getId());
            assertThat(item.title()).isEqualTo("Pendiente");
            assertThat(item.readAt()).isNull();
        });

        assertThat(notifications.markAllAsRead(recipient.getId(), Instant.now())).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
        assertThat(notifications.countByRecipientUserIdAndReadAtIsNull(recipient.getId())).isZero();
    }

    @Test
    void requestProjectionsAndDashboardCounts_followPersistedState() {
        var before = dashboard.getCounts();
        User requester = persistUser("requester-repo@example.com", "Solicitante", UserRole.EDITOR);
        requester.setCountry("España");
        persistSurvey(requester, "DASHBOARD001", "Encuesta dashboard");
        RegistrationRequest registration = persist(RegistrationRequest.builder()
            .email("pending-repo@example.com")
            .name("Pendiente")
            .institution("Centro")
            .country("España")
            .motivation("Crear encuestas")
            .passwordHash("hash")
            .build());
        ElementRequest element = persist(ElementRequest.builder()
            .requestedBy(requester)
            .elementType(RequestedElementType.BLOCK)
            .message("Nuevo bloque")
            .requestPayload(Map.of("name", "Bloque"))
            .build());
        entityManager.flush();

        assertThat(registrationRequests.findAllResponses()).extracting(item -> item.id())
            .contains(registration.getId());
        assertThat(registrationRequests.findAllResponses()).filteredOn(item -> item.id().equals(registration.getId()))
            .singleElement().extracting(item -> item.country()).isEqualTo("España");
        assertThat(elementRequests.findAllResponses()).filteredOn(item -> item.id().equals(element.getId()))
            .singleElement().satisfies(item -> {
                assertThat(item.requestedByUserId()).isEqualTo(requester.getId());
                assertThat(item.requestedByCountry()).isEqualTo("España");
                assertThat(item.requestPayload()).containsEntry("name", "Bloque");
            });

        var after = dashboard.getCounts();
        assertThat(after.surveyCount()).isEqualTo(before.surveyCount() + 1);
        assertThat(after.pendingRegistrationRequests()).isEqualTo(before.pendingRegistrationRequests() + 1);
        assertThat(after.pendingElementRequests()).isEqualTo(before.pendingElementRequests() + 1);
    }

    private Notification persistNotification(User recipient, String title) {
        return persist(Notification.builder()
            .recipientUser(recipient)
            .type(NotificationType.GENERAL)
            .channel(NotificationChannel.INTERNAL)
            .title(title)
            .message("Mensaje")
            .targetType(NotificationTargetType.NONE)
            .build());
    }

    private Survey persistSurvey(User owner, String publicCode, String title) {
        return persist(Survey.builder()
            .publicCode(publicCode)
            .owner(owner)
            .title(title)
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build());
    }

    private User persistUser(String email, String name, UserRole role) {
        return persist(User.builder()
            .role(role)
            .email(email)
            .name(name)
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
