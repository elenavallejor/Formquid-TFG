package com.formquid.repository;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockScope;
import com.formquid.block.repository.BlockRepository;
import com.formquid.config.PostgresDataJpaTest;
import com.formquid.content.entity.ContentItem;
import com.formquid.content.entity.ContentItemType;
import com.formquid.content.repository.ContentItemRepository;
import com.formquid.question.entity.EvaluationDirection;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import com.formquid.question.entity.QuestionType;
import com.formquid.question.repository.QuestionRepository;
import com.formquid.template.entity.Template;
import com.formquid.template.repository.TemplateRepository;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserGender;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresDataJpaTest
class CatalogRepositoryTest {

    @Autowired private BlockRepository blocks;
    @Autowired private QuestionRepository questions;
    @Autowired private TemplateRepository templates;
    @Autowired private ContentItemRepository contentItems;
    @Autowired private EntityManager entityManager;

    @Test
    void catalogSummaries_filterByCreatorAndCountQuestions() {
        User creator = persistUser("catalog-owner@example.com", "Creadora");
        User other = persistUser("catalog-other@example.com", "Otra persona");

        Block block = persist(Block.builder()
            .scope(BlockScope.CATALOG).name("Bloque propio").createdBy(creator).build());
        persist(ContentItem.builder()
            .parentBlock(block).itemType(ContentItemType.QUESTION).position(1).required(true).build());

        persist(Question.builder()
            .scope(QuestionScope.CATALOG_FIXED)
            .statement("Pregunta propia")
            .questionType(QuestionType.TEXT)
            .evaluationDirection(EvaluationDirection.NEUTRAL)
            .createdBy(creator)
            .build());

        Template template = persist(Template.builder()
            .title("Plantilla propia").createdBy(creator).build());
        persist(ContentItem.builder()
            .template(template).itemType(ContentItemType.QUESTION).position(1).required(false).build());
        entityManager.flush();

        assertThat(blocks.findCatalogSummaries(BlockScope.CATALOG, creator.getId(), true, PageRequest.of(0, 10)))
            .singleElement().satisfies(summary -> {
                assertThat(summary.name()).isEqualTo("Bloque propio");
                assertThat(summary.questionCount()).isEqualTo(1);
                assertThat(summary.creatorName()).isEqualTo("Creadora");
            });
        assertThat(questions.findCatalogSummaries(QuestionScope.CATALOG_FIXED, creator.getId(), true, PageRequest.of(0, 10)))
            .singleElement().extracting(summary -> summary.statement()).isEqualTo("Pregunta propia");
        assertThat(templates.findCatalogSummaries(creator.getId(), true, PageRequest.of(0, 10)))
            .singleElement().satisfies(summary -> {
                assertThat(summary.title()).isEqualTo("Plantilla propia");
                assertThat(summary.questionCount()).isEqualTo(1);
            });

        assertThat(blocks.findCatalogSummaries(BlockScope.CATALOG, other.getId(), false, PageRequest.of(0, 10)))
            .extracting(summary -> summary.id()).contains(block.getId());
    }

    @Test
    void contentRows_preservePositionAndParentIdentity() {
        User creator = persistUser("content-owner@example.com", "Creadora");
        Block block = persist(Block.builder().scope(BlockScope.CATALOG).name("Ordenado").createdBy(creator).build());
        ContentItem second = persist(ContentItem.builder()
            .parentBlock(block).itemType(ContentItemType.TEXT).position(2).textBody("Segundo").build());
        ContentItem first = persist(ContentItem.builder()
            .parentBlock(block).itemType(ContentItemType.TEXT).position(1).textBody("Primero").build());
        entityManager.flush();

        var rows = contentItems.findBlockContentRows(List.of(block.getId()));

        assertThat(rows).extracting(row -> row.id()).containsExactly(first.getId(), second.getId());
        assertThat(rows).extracting(row -> row.textBody()).containsExactly("Primero", "Segundo");
        assertThat(rows).allSatisfy(row -> assertThat(row.parentBlockId()).isEqualTo(block.getId()));
    }

    private User persistUser(String email, String name) {
        return persist(User.builder()
            .role(UserRole.EDITOR)
            .email(email)
            .name(name)
            .gender(UserGender.OTHER)
            .institution("FormQuid")
            .passwordHash("not-used-in-tests")
            .build());
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
