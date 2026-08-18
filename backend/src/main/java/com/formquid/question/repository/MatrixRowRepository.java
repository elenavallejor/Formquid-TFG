package com.formquid.question.repository;

import com.formquid.question.entity.MatrixRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatrixRowRepository extends JpaRepository<MatrixRow, UUID> {

    List<MatrixRow> findByQuestionIdOrderByPositionAsc(String questionId);
}
