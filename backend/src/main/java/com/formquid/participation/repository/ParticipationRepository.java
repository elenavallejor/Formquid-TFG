package com.formquid.participation.repository;

import com.formquid.participation.dto.ParticipationSummaryResponseDto;
import com.formquid.participation.entity.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    boolean existsBySurveyId(Long surveyId);

    boolean existsBySurveyIdAndRespondentUserId(Long surveyId, Long userId);

    boolean existsBySurveyIdAndRespondentTokenHash(Long surveyId, String tokenHash);

    @Query(
        value = """
            select new com.formquid.participation.dto.ParticipationSummaryResponseDto(
                participation.id,
                participation.submittedAt
            )
            from Participation participation
            where participation.survey.id = :surveyId
            """,
        countQuery = """
            select count(participation.id)
            from Participation participation
            where participation.survey.id = :surveyId
            """
    )
    Page<ParticipationSummaryResponseDto> findSummariesBySurveyId(
        @Param("surveyId") Long surveyId,
        Pageable pageable
    );

    @Query("""
        select participation
        from Participation participation
        join fetch participation.survey
        where participation.id = :participationId
            and participation.survey.id = :surveyId
        """)
    Optional<Participation> findDetailByIdAndSurveyId(
        @Param("participationId") Long participationId,
        @Param("surveyId") Long surveyId
    );
}
