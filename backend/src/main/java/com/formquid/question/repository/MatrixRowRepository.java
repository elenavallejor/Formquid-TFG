package com.formquid.question.repository;

import com.formquid.question.entity.MatrixRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatrixRowRepository extends JpaRepository<MatrixRow, Long> {

    List<MatrixRow> findByQuestionIdOrderByPositionAsc(Long questionId);
}