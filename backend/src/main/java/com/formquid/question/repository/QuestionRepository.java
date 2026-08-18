package com.formquid.question.repository;

import com.formquid.question.entity.Question;
import com.formquid.question.entity.QuestionScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByScopeOrderByCatalogNameAsc(QuestionScope scope);

    Optional<Question> findByParentItemId(UUID parentItemId);

    long countBySourceFixedQuestionId(String sourceFixedQuestionId);
}
