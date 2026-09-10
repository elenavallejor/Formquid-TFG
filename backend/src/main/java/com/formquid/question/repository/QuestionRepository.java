package com.formquid.question.repository;

import com.formquid.question.dto.FixedQuestionSummaryDto;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Proporciona el acceso persistente a las preguntas y sus consultas de catálogo.
 */
public interface QuestionRepository extends JpaRepository<Question, String> {

    @Query(
        value = """
            select new com.formquid.question.dto.FixedQuestionSummaryDto(
                question.id,
                question.statement,
                question.questionType,
                creator.name,
                question.updatedAt
            )
            from Question question
            left join question.createdBy creator
            where question.scope = :scope
            """,
        countQuery = """
            select count(question.id)
            from Question question
            where question.scope = :scope
            """
    )
    Page<FixedQuestionSummaryDto> findCatalogSummaries(
        @Param("scope") QuestionScope scope,
        Pageable pageable
    );

    @Query(
        value = """
            select question
            from Question question
            left join fetch question.settings
            left join fetch question.createdBy
            where question.scope = :scope
            """,
        countQuery = """
            select count(question.id)
            from Question question
            where question.scope = :scope
            """
    )
    Page<Question> findCatalogQuestionsWithDetails(
        @Param("scope") QuestionScope scope,
        Pageable pageable
    );

    Optional<Question> findByParentItemId(UUID parentItemId);

    long countBySourceFixedQuestionId(String sourceFixedQuestionId);
}