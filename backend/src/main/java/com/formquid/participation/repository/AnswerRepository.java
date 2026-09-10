package com.formquid.participation.repository;

import com.formquid.participation.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByParticipationId(Long participationId);

    List<Answer> findByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);

    long countByQuestionId(Long questionId);

    @Query("select avg(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    Double averageNumeric(@Param("questionId") Long questionId);

    @Query("select min(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    BigDecimal minNumeric(@Param("questionId") Long questionId);

    @Query("select max(answer.numericValue) from Answer answer where answer.question.id = :questionId and answer.numericValue is not null")
    BigDecimal maxNumeric(@Param("questionId") Long questionId);
}
