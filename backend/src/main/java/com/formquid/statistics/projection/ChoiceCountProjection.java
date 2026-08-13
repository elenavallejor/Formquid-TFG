package com.formquid.statistics.projection;

/**
 * Proyección para el recuento de opciones seleccionadas.
 */
public interface ChoiceCountProjection {

    String getStoredValue();

    Long getAnswerCount();
}