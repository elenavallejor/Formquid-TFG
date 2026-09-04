package com.formquid.question.repository;

import com.formquid.question.dto.FixedQuestionSummaryDto;
import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByScopeOrderByCatalogNameAsc(QuestionScope scope);

    @Query(
        value = """
            select new com.formquid.question.dto.FixedQuestionSummaryDto(
                question.id,
                question.catalogName,
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

    Optional<Question> findByParentItemId(UUID parentItemId);

    long countBySourceFixedQuestionId(String sourceFixedQuestionId);
}