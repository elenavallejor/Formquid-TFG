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
        join fetch value.matrixRow row
        where value.answer.id in :answerIds
        order by value.answer.id asc, row.position asc
    """)
    List<MatrixAnswerValue> findByAnswerIdsWithRow(@Param("answerIds") List<Long> answerIds);
}
