package com.formquid.survey.entity;

import com.formquid.block.entity.Block;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un bloque o grupo de preguntas dentro de una encuesta.
 *
 * Puede estar asociado a un bloque reutilizable o ser un grupo sin bloque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "survey_blocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_survey_blocks_survey_position",
            columnNames = {"survey_id", "position"}
        )
    },
    indexes = {
        @Index(name = "idx_survey_blocks_source_block_id", columnList = "source_block_id")
    }
)
public class SurveyBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_block_id")
    private Block sourceBlock;

    @Column(length = 180)
    private String title;

    @Column(nullable = false)
    private Integer position;

    /**
     * Preguntas incluidas en este bloque.
     *
     * Se usa @BatchSize para que Hibernate cargue varias colecciones
     * de preguntas en lotes y no una por una.
     */
    @OneToMany(
        mappedBy = "surveyBlock",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("position ASC")
    @BatchSize(size = 30)
    @Builder.Default
    private List<SurveyQuestion> questions = new ArrayList<>();

    public void addQuestion(SurveyQuestion question) {
        questions.add(question);
        question.setSurveyBlock(this);
    }
}