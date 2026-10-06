package com.formquid.participation.repository;

import com.formquid.participation.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByParticipationId(Long participationId);

    @Query("""
        select answer
        from Answer answer
        join fetch answer.question question
        left join fetch question.settings
        where answer.participation.id = :participationId
        order by answer.id asc
    """)
    List<Answer> findDetailsByParticipationId(@Param("participationId") Long participationId);
}
