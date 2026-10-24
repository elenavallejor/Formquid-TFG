package com.formquid.survey.repository;

import com.formquid.common.Category;
import com.formquid.config.PostgresDataJpaTest;
import com.formquid.survey.entity.Survey;
import com.formquid.survey.entity.SurveyAccessMode;
import com.formquid.survey.entity.SurveyLifecycle;
import com.formquid.survey.entity.SurveyVisibility;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        var firstPage = surveys.findListedOpenPage(
            false, "", "", "clima", "salud-mental", PageRequest.of(0, 1)
        );
        var secondPage = surveys.findListedOpenPage(
            false, "", "", "clima", "salud-mental", PageRequest.of(1, 1)
        );
        var registeredPage = surveys.findListedOpenPage(
            true, "AUTHENTICATED", "", "clima", "salud-mental", PageRequest.of(0, 1)
        );

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getContent().get(0).getId())
            .isNotEqualTo(secondPage.getContent().get(0).getId());
        assertThat(registeredPage.getTotalElements()).isEqualTo(1);
        assertThat(registeredPage.getContent().get(0).getTitle()).isEqualTo("Clima familiar");

        var tagSearchPage = surveys.findListedOpenPage(
            false, "PUBLIC", "", "salud mental", "", PageRequest.of(0, 2)
        );
        assertThat(tagSearchPage.getTotalElements()).isEqualTo(3);
        assertThat(tagSearchPage.getContent()).hasSize(2);
    }

    @Test
    void publicListingUsesIdAsStableTieBreakerWhenCreationTimesMatch() {
        User owner = User.builder()
            .role(UserRole.EDITOR)
            .email("stable-public-pages@example.com")
            .name("Editora")
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build();
        entityManager.persist(owner);

        Survey first = persistSurvey(
            owner, "STABLEPAGE01", "Primera", "educacion", SurveyAccessMode.PUBLIC
        );
        Survey second = persistSurvey(
            owner, "STABLEPAGE02", "Segunda", "educacion", SurveyAccessMode.PUBLIC
        );
        Survey third = persistSurvey(
            owner, "STABLEPAGE03", "Tercera", "educacion", SurveyAccessMode.PUBLIC
        );
        entityManager.flush();
        entityManager.createNativeQuery("""
                update surveys
                set created_at = timestamp with time zone '2026-01-01 10:00:00+00'
                where owner_id = :ownerId
                """)
            .setParameter("ownerId", owner.getId())
            .executeUpdate();
        entityManager.clear();

        var firstPage = surveys.findListedOpenPage(
            false, "", "", "", "", PageRequest.of(0, 2)
        );
        var secondPage = surveys.findListedOpenPage(
            false, "", "", "", "", PageRequest.of(1, 2)
        );

        assertThat(firstPage.getContent()).extracting(Survey::getId)
            .containsExactly(third.getId(), second.getId());
        assertThat(secondPage.getContent()).extracting(Survey::getId)
            .containsExactly(first.getId());
    }

    @Test
    void databaseAllowsTenSurveyTags() {
        User owner = User.builder()
            .role(UserRole.EDITOR)
            .email("ten-tags@example.com")
            .name("Editora")
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build();
        entityManager.persist(owner);
        String[] tags = IntStream.rangeClosed(1, Survey.MAX_TAGS)
            .mapToObj(index -> "etiqueta-" + index)
            .toArray(String[]::new);
        Survey survey = Survey.builder()
            .owner(owner)
            .title("Encuesta con diez etiquetas")
            .tags(tags)
            .lifecycleStatus(SurveyLifecycle.DRAFT)
            .accessMode(SurveyAccessMode.PUBLIC)
            .visibility(SurveyVisibility.UNLISTED)
            .build();

        entityManager.persist(survey);
        entityManager.flush();
        entityManager.clear();

        assertThat(surveys.findById(survey.getId()).orElseThrow().getTags())
            .containsExactly(tags);
    }

    @Test
    void surveyCategoryFilterAndDatabaseConstraint() {
        User owner = User.builder()
            .role(UserRole.EDITOR)
            .email("category-owner@example.com")
            .name("Editora")
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build();
        entityManager.persist(owner);
        Survey smartphone = persistSurvey(owner, "CATEGORY0001", "Teléfonos", "móvil", SurveyAccessMode.PUBLIC);
        smartphone.setCategory(Category.TELEFONO_INTELIGENTE);
        Survey computers = persistSurvey(owner, "CATEGORY0002", "Ordenadores", "móvil", SurveyAccessMode.PUBLIC);
        computers.setCategory(Category.ORDENADORES);
        persistSurvey(owner, "CATEGORY0003", "Otros", "móvil", SurveyAccessMode.PUBLIC);
        entityManager.flush();

        var page = surveys.findListedOpenPage(
            false, "", "TELEFONO_INTELIGENTE,ORDENADORES", "móvil", "", PageRequest.of(0, 1)
        );
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Survey::getId).containsAnyOf(smartphone.getId(), computers.getId());
        var allMatches = surveys.findListedOpenPage(
            false, "", "TELEFONO_INTELIGENTE,ORDENADORES", "móvil", "", PageRequest.of(0, 9)
        );
        assertThat(allMatches.getContent()).extracting(Survey::getId)
            .containsExactlyInAnyOrder(smartphone.getId(), computers.getId());

        assertThatThrownBy(() -> entityManager.createNativeQuery(
            "update surveys set category = 'DATOS_PERSONALES' where id = :id"
        ).setParameter("id", smartphone.getId()).executeUpdate())
            .isInstanceOf(RuntimeException.class);
    }

    private Survey persistSurvey(User owner, String publicCode, String title, String tag, SurveyAccessMode accessMode) {
        Survey survey = Survey.builder()
            .publicCode(publicCode)
            .owner(owner)
            .title(title)
            .tags(new String[] { tag })
            .lifecycleStatus(SurveyLifecycle.OPEN)
            .accessMode(accessMode)
            .visibility(SurveyVisibility.LISTED)
            .build();
        entityManager.persist(survey);
        return survey;
    }
}
