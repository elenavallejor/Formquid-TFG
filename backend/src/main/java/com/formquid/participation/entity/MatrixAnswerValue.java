package com.formquid.participation.entity;

import com.formquid.question.entity.MatrixRow;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "matrix_answer_values")
public class MatrixAnswerValue {

    @EmbeddedId
    private MatrixAnswerValueId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("answerId")
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("matrixRowId")
    @JoinColumn(name = "matrix_row_id", nullable = false)
    private MatrixRow matrixRow;

    @Column(name = "numeric_value", nullable = false)
    private BigDecimal numericValue;
}
