package com.formquid.participation.repository;

import com.formquid.participation.entity.MatrixAnswerValue;
import com.formquid.participation.entity.MatrixAnswerValueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatrixAnswerValueRepository extends JpaRepository<MatrixAnswerValue, MatrixAnswerValueId> {

    @Query("""
        select value
        from MatrixAnswerValue value
        where value.answer.question.id = :questionId
        order by value.matrixRow.position asc
    """)
    List<MatrixAnswerValue> findByQuestionId(@Param("questionId") String questionId);
}
