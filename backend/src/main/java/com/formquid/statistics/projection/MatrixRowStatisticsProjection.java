package com.formquid.statistics.projection;

import java.math.BigDecimal;

/**
 * Proyección de estadísticas agregadas para una fila de matriz.
 */
public interface MatrixRowStatisticsProjection {

    String getRowKey();

    Long getAnswerCount();

    BigDecimal getAverage();

    BigDecimal getMinimum();

    BigDecimal getMaximum();
}
