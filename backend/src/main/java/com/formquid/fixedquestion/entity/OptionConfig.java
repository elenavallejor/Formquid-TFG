package com.formquid.fixedquestion.entity;

import lombok.*;

/**
 * Representa una opción de respuesta de una pregunta fija.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionConfig {

    /**
     * Texto visible de la opción.
     */
    private String label;

    /**
     * Valor interno almacenado al seleccionar la opción.
     */
    private String storedValue;

    /**
     * Valor numérico opcional para estadísticas.
     */
    private Integer scoreValue;

    /**
     * Indica si la opción representa una respuesta no evaluable,
     * por ejemplo "No lo sé".
     */
    private boolean nonResponseOption;

    /**
     * Indica si esta opción permite texto libre,
     * por ejemplo "Otra".
     */
    private boolean allowsFreeText;
}