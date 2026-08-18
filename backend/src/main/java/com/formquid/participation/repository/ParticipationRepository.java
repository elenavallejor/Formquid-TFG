package com.formquid.participation.repository;

import com.formquid.participation.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParticipationRepository extends JpaRepository<Participation, UUID> {

    long countBySurveyId(String surveyId);

    boolean existsBySurveyId(String surveyId);

    List<Participation> findBySurveyIdOrderBySubmittedAtAsc(String surveyId);
}
