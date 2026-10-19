package com.formquid.content.entity;

import com.formquid.block.entity.Block;
import com.formquid.question.entity.Question;
import com.formquid.survey.entity.Survey;
import com.formquid.template.entity.Template;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "content_items",
    check = @CheckConstraint(constraint = """
        item_type in ('QUESTION', 'BLOCK', 'TEXT')
        and position >= 1
        and (
            (survey_id is not null)::int
            + (template_id is not null)::int
            + (parent_block_id is not null)::int
        ) = 1
        and (
            (item_type = 'QUESTION' and is_required is not null and text_body is null)
            or (item_type = 'BLOCK' and is_required is null and text_body is null)
            or (item_type = 'TEXT' and is_required is null and text_body is not null)
        )
    """),
    indexes = {
        @Index(name = "idx_content_items_survey", columnList = "survey_id, position"),
        @Index(name = "idx_content_items_template", columnList = "template_id, position"),
        @Index(name = "idx_content_items_block", columnList = "parent_block_id, position")
    }
)
public class ContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id")
    private Survey survey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_block_id")
    private Block parentBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ContentItemType itemType;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "is_required")
    private Boolean required;

    @Column(name = "text_body", columnDefinition = "text")
    private String textBody;

    @OneToOne(mappedBy = "parentItem", fetch = FetchType.LAZY)
    private Question question;

    @OneToOne(mappedBy = "parentItem", fetch = FetchType.LAZY)
    private Block block;

    @PrePersist
    protected void onCreate() {
        if (itemType == ContentItemType.QUESTION && required == null) {
            required = false;
        }
    }
}