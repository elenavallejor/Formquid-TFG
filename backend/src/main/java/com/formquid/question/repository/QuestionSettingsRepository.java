package com.formquid.question.repository;

import com.formquid.question.entity.QuestionSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionSettingsRepository extends JpaRepository<QuestionSettings, Long> {
}