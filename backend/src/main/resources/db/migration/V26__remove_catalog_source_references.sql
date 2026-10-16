DROP TRIGGER trg_blocks_catalog_source ON blocks;
DROP TRIGGER trg_questions_catalog_source ON questions;
DROP FUNCTION validate_catalog_source_reference();

ALTER TABLE blocks
    DROP CONSTRAINT ck_blocks_values,
    DROP COLUMN source_block_id,
    ADD CONSTRAINT ck_blocks_values CHECK (
        scope IN ('CATALOG', 'SNAPSHOT') AND (
            (scope = 'CATALOG' AND parent_item_id IS NULL)
            OR (scope = 'SNAPSHOT' AND parent_item_id IS NOT NULL)
        )
    );

ALTER TABLE questions
    DROP CONSTRAINT ck_questions_values,
    DROP COLUMN source_fixed_question_id,
    ADD CONSTRAINT ck_questions_values CHECK (
        scope IN ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
        AND question_type IN ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN')
        AND evaluation_direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
        AND (
            (scope = 'CATALOG_FIXED' AND parent_item_id IS NULL)
            OR (scope IN ('LOCAL', 'FIXED_SNAPSHOT') AND parent_item_id IS NOT NULL)
        )
        AND (
            (evaluation_direction = 'NEUTRAL' AND evaluation_weight IS NULL)
            OR (evaluation_direction IN ('POSITIVE', 'NEGATIVE') AND evaluation_weight > 0)
        )
    );
