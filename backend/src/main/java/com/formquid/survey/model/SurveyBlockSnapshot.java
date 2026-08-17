package com.formquid.survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Bloque incluido en el snapshot de una encuesta o plantilla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyBlockSnapshot {

    /**
     * Clave estable del bloque dentro del snapshot.
     */
    private String blockKey;

    /**
     * Bloque de catálogo usado como origen, si existe.
     */
    private Long sourceBlockId;

    private String title;
    private Integer position;

    @Builder.Default
    private List<SurveyQuestionSnapshot> questions = new ArrayList<>();
}
