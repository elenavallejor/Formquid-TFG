package com.formquid.statistics.projection;

/**
 * Proyección para el recuento de respuestas booleanas.
 */
public interface BooleanStatisticsProjection {

    Long getYesCount();

    Long getNoCount();
}