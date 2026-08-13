package com.formquid.survey.repository;

import com.formquid.survey.entity.SurveyBlock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de acceso a datos para bloques de encuesta.
 */
public interface SurveyBlockRepository extends JpaRepository<SurveyBlock, Long> {
}