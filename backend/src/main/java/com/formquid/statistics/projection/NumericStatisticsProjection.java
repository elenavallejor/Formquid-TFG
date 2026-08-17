package com.formquid.statistics.projection;

import java.math.BigDecimal;

/**
 * Proyección de estadísticas numéricas calculadas en base de datos.
 */
public interface NumericStatisticsProjection {

    Long getAnswerCount();

    BigDecimal getAverage();

    BigDecimal getMinimum();

    BigDecimal getMaximum();
}
