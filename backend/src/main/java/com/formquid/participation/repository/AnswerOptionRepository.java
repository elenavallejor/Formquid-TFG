package com.formquid.participation.repository;

import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, AnswerOptionId> {

    @Query("""
        select selected
        from AnswerOption selected
        join fetch selected.option option
        where selected.answer.id in :answerIds
        order by selected.answer.id asc, option.position asc
    """)
    List<AnswerOption> findByAnswerIdsWithOption(@Param("answerIds") List<Long> answerIds);
}
