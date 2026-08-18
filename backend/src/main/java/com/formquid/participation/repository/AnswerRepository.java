package com.formquid.participation.repository;

import com.formquid.participation.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    List<Answer> findByParticipationId(UUID participationId);

    List<Answer> findByQuestionId(String questionId);

    boolean existsByQuestionId(String questionId);

    long countByQuestionId(String questionId);

    @Query("select avg(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    Double averageNumeric(@Param("questionId") String questionId);

    @Query("select min(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    BigDecimal minNumeric(@Param("questionId") String questionId);

    @Query("select max(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    BigDecimal maxNumeric(@Param("questionId") String questionId);
}
