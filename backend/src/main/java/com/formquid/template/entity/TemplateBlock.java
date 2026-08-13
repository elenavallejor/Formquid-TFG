package com.formquid.template.entity;

import com.formquid.block.entity.Block;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un grupo de preguntas dentro de una plantilla.
 *
 * Puede estar asociado a un bloque reutilizable o representar un grupo
 * de preguntas sin bloque.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "template_blocks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_template_blocks_template_block",
            columnNames = {"template_id", "block_id"}
        ),
        @UniqueConstraint(
            name = "uk_template_blocks_template_position",
            columnNames = {"template_id", "position"}
        )
    },
    indexes = {
        @Index(
            name = "idx_template_blocks_block_id",
            columnList = "block_id"
        )
    }
)
public class TemplateBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Plantilla a la que pertenece este grupo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private SurveyTemplate template;

    /**
     * Bloque reutilizable asociado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    /**
     * Posición del grupo dentro de la plantilla.
     */
    private Integer position;

    /**
     * Preguntas incluidas en este grupo.
     */
    @OneToMany(
        mappedBy = "templateBlock",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("position ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<TemplateQuestion> questions = new ArrayList<>();

    /**
     * Añade una pregunta al grupo manteniendo la relación bidireccional.
     */
    public void addQuestion(TemplateQuestion question) {
        questions.add(question);
        question.setTemplateBlock(this);
    }
}