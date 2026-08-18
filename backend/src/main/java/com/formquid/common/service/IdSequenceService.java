package com.formquid.common.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdSequenceService {

    private final EntityManager entityManager;

    public String nextSurveyId() {
        return next("S", "survey_id_sequence");
    }

    public String nextBlockId() {
        return next("B", "block_id_sequence");
    }

    public String nextQuestionId() {
        return next("Q", "question_id_sequence");
    }

    /**
     * Recibe una secuencia interna y devuelve el identificador público.
     */
    private String next(String prefix, String sequenceName) {
        Number value = (Number) entityManager
            .createNativeQuery("select nextval('" + sequenceName + "')")
            .getSingleResult();
        return prefix + value.longValue();
    }
}
