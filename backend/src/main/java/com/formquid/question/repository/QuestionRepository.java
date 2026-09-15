package com.formquid.question.repository;

import com.formquid.question.dto.FixedQuestionSummaryDto;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Proporciona el acceso persistente a las preguntas y sus consultas de catálogo.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query(
        value = """
            select new com.formquid.question.dto.FixedQuestionSummaryDto(
                question.id,
                question.statement,
                question.questionType,
                creator.name,
                question.createdAt,
                question.updatedAt
            )
            from Question question
            left join question.createdBy creator
            where question.scope = :scope
                and (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
            """,
        countQuery = """
            select count(question.id)
            from Question question
            left join question.createdBy creator
            where question.scope = :scope
                and (
                    (:mine = true and creator.id = :userId)
                    or (:mine = false and (creator.id is null or creator.id <> :userId))
                )
            """
    )
    Page<FixedQuestionSummaryDto> findCatalogSummaries(
        @Param("scope") QuestionScope scope,
        @Param("userId") Long userId,
        @Param("mine") boolean mine,
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

    @Query("""
        select distinct question
        from Question question
        left join fetch question.settings
        left join fetch question.sourceFixedQuestion
        left join fetch question.options
        where question.parentItem.id in :parentItemIds
        """)
    List<Question> findByParentItemIdsWithDetails(
        @Param("parentItemIds") Collection<Long> parentItemIds
    );

    Optional<Question> findByParentItemId(Long parentItemId);

    long countBySourceFixedQuestionId(Long sourceFixedQuestionId);
}
