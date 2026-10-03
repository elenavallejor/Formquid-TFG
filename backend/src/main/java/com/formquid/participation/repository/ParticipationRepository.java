package com.formquid.participation.repository;

import com.formquid.participation.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    long countBySurveyId(Long surveyId);

    boolean existsBySurveyId(Long surveyId);

    boolean existsBySurveyIdAndRespondentUserId(Long surveyId, Long userId);

    boolean existsBySurveyIdAndRespondentTokenHash(Long surveyId, String tokenHash);

    List<Participation> findBySurveyIdOrderBySubmittedAtAsc(Long surveyId);
}
