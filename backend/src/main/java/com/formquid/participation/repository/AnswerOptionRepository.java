package com.formquid.participation.repository;

import com.formquid.participation.entity.AnswerOption;
import com.formquid.participation.entity.AnswerOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, AnswerOptionId> {

    @Query("""
        select option.id, option.label, count(selected.answer.id)
        from QuestionOption option
        left join AnswerOption selected on selected.option.id = option.id
        where option.question.id = :questionId
        group by option.id, option.label, option.position
        order by option.position asc
    """)
    List<Object[]> countOptionsByQuestion(@Param("questionId") Long questionId);
}
