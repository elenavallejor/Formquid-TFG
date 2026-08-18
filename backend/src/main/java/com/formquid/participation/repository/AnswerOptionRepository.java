package com.formquid.participation.repository;

import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, AnswerOptionId> {

    @Query("""
        select selected.option.id, selected.option.label, count(selected)
        from AnswerOption selected
        where selected.answer.question.id = :questionId
        group by selected.option.id, selected.option.label, selected.option.position
        order by selected.option.position asc
    """)
    List<Object[]> countOptionsByQuestion(@Param("questionId") String questionId);
}
