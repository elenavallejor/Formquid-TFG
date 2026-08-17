package com.formquid.question.entity;

import lombok.*;

/**
 * Representa una fila de una pregunta tipo matriz.
 *
 * Se almacena como parte de un array JSON dentro de Question.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatrixRowConfig {

    /**
     * Clave interna de la fila.
     */
    private String key;

    /**
     * Texto visible de la fila.
     */
    private String label;
}
