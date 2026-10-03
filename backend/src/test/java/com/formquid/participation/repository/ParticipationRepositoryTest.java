package com.formquid.participation.repository;

import com.formquid.config.PostgresDataJpaTest;
import com.formquid.participation.entity.Participation;
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
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@PostgresDataJpaTest
class ParticipationRepositoryTest {

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndFlush_whenAnonymousRespondentSubmitsTwice_shouldRejectSecondParticipation() {
        Survey survey = persistSurvey();

        participationRepository.saveAndFlush(Participation.builder()
            .survey(survey)
            .respondentTokenHash("a".repeat(64))
            .build());

        assertThatThrownBy(() -> participationRepository.saveAndFlush(Participation.builder()
            .survey(survey)
            .respondentTokenHash("a".repeat(64))
            .build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenAuthenticatedRespondentSubmitsTwice_shouldRejectSecondParticipation() {
        User respondent = persistUser("respondent@formquid.test");
        Survey survey = persistSurvey();

        participationRepository.saveAndFlush(Participation.builder()
            .survey(survey)
            .respondentUser(respondent)
            .build());

        assertThatThrownBy(() -> participationRepository.saveAndFlush(Participation.builder()
            .survey(survey)
            .respondentUser(respondent)
            .build()))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Survey persistSurvey() {
        User owner = persistUser("owner-" + System.nanoTime() + "@formquid.test");
        Survey survey = Survey.builder()
            .publicCode("ABCDEF234567")
            .owner(owner)
            .title("Encuesta de prueba")
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.LISTED)
            .build();
        entityManager.persist(survey);
        entityManager.flush();
        return survey;
    }

    private User persistUser(String email) {
        User user = User.builder()
            .role(UserRole.EDITOR)
            .email(email)
            .name("Usuario de prueba")
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}
