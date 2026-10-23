ALTER TABLE questions DROP CONSTRAINT ck_questions_values;

ALTER TABLE questions ADD CONSTRAINT ck_questions_values CHECK (
    scope IN ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
    AND question_type IN ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN')
    AND evaluation_direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
    AND (
        (scope = 'CATALOG_FIXED' AND parent_item_id IS NULL)
        OR (scope IN ('LOCAL', 'FIXED_SNAPSHOT') AND parent_item_id IS NOT NULL)
    )
    AND (
        (evaluation_direction = 'NEUTRAL' AND evaluation_weight IS NULL)
        OR (evaluation_direction = 'POSITIVE' AND evaluation_weight > 0)
        OR (evaluation_direction = 'NEGATIVE' AND evaluation_weight < 0)
    )
) NOT VALID;

UPDATE questions
SET evaluation_weight = -evaluation_weight
WHERE evaluation_direction = 'NEGATIVE' AND evaluation_weight > 0;
