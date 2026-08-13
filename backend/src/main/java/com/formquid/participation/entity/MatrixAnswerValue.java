package com.formquid.participation.entity;

import lombok.*;

/**
 * Representa el valor respondido en una fila de una pregunta de matriz.
 *
 * Se almacena como parte del JSONB de Answer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatrixAnswerValue {

    /**
     * Clave de la fila de matriz.
     */
    private String rowKey;

    /**
     * Valor seleccionado o introducido en esa fila.
     */
    private String value;

    /**
     * Valor numérico asociado, útil para estadísticas.
     */
    private Integer scoreValue;
}