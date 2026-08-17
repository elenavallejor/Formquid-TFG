package com.formquid.survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Estructura completa de una encuesta o plantilla.
 *
 * Se almacena como JSONB para reducir tablas intermedias sin perder una forma
 * explícita y documentada de bloques y preguntas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyStructure {

    @Builder.Default
    private List<SurveyBlockSnapshot> blocks = new ArrayList<>();

    /**
     * Devuelve todas las preguntas en orden de bloque y posición.
     */
    public List<SurveyQuestionSnapshot> getQuestions() {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }

        return blocks.stream()
            .filter(block -> block.getQuestions() != null)
            .flatMap(block -> block.getQuestions().stream())
            .toList();
    }

    /**
     * Indica si la estructura contiene al menos una pregunta.
     */
    public boolean hasQuestions() {
        return !getQuestions().isEmpty();
    }

    /**
     * Busca una pregunta por su clave estable dentro del snapshot.
     */
    public Optional<SurveyQuestionSnapshot> findQuestion(String questionKey) {
        if (questionKey == null || questionKey.isBlank()) {
            return Optional.empty();
        }

        return getQuestions()
            .stream()
            .filter(question -> questionKey.equals(question.getQuestionKey()))
            .findFirst();
    }
}
