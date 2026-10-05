package com.formquid.survey.repository;

import com.formquid.config.PostgresDataJpaTest;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresDataJpaTest
class MainPageSurveyRepositoryTest {

    @Autowired private SurveyRepository surveys;
    @Autowired private EntityManager entityManager;

    @Test
    void tagAndNameAreCombinedBeforePaginationAndVisibilityRules() {
        User owner = User.builder()
            .role(UserRole.EDITOR)
            .email("main-page-filter@example.com")
            .name("Editora")
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build();
        entityManager.persist(owner);

        persistSurvey(owner, "MAINPAGE0001", "Encuesta de clima", "salud-mental", SurveyAccessMode.PUBLIC);
        persistSurvey(owner, "MAINPAGE0002", "Clima laboral", "salud-mental", SurveyAccessMode.PUBLIC);
        persistSurvey(owner, "MAINPAGE0003", "Clima escolar", "educacion", SurveyAccessMode.PUBLIC);
        persistSurvey(owner, "MAINPAGE0004", "Bienestar general", "salud-mental", SurveyAccessMode.PUBLIC);
        persistSurvey(owner, "MAINPAGE0005", "Clima familiar", "salud-mental", SurveyAccessMode.AUTHENTICATED);
        entityManager.flush();
        entityManager.clear();

        var firstPage = surveys.findListedOpenPageByTag(
            false, "", "clima", "salud-mental", PageRequest.of(0, 1)
        );
        var secondPage = surveys.findListedOpenPageByTag(
            false, "", "clima", "salud-mental", PageRequest.of(1, 1)
        );
        var registeredPage = surveys.findListedOpenPageByTag(
            true, "AUTHENTICATED", "clima", "salud-mental", PageRequest.of(0, 1)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().get(0).getId())
            .isNotEqualTo(secondPage.getContent().get(0).getId());
        assertThat(registeredPage.getTotalElements()).isEqualTo(1);
        assertThat(registeredPage.getContent().get(0).getTitle()).isEqualTo("Clima familiar");
    }

    private void persistSurvey(User owner, String publicCode, String title, String tag, SurveyAccessMode accessMode) {
        entityManager.persist(Survey.builder()
            .publicCode(publicCode)
            .owner(owner)
            .title(title)
            .tags(new String[] { tag })
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(accessMode)
            .visibility(SurveyVisibility.LISTED)
            .build());
    }
}
