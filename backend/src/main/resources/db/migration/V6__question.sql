BEGIN;

ALTER TABLE questions
DROP CONSTRAINT IF EXISTS questions_question_type_check;

UPDATE questions
SET question_type = CASE
    WHEN question_type IN ('SHORT_TEXT', 'LONG_TEXT') THEN 'TEXT'
    WHEN question_type = 'BIPOLAR_SCALE' THEN 'SCALE'
    ELSE question_type
END
WHERE question_type IN ('SHORT_TEXT', 'LONG_TEXT', 'BIPOLAR_SCALE');

ALTER TABLE questions
ADD CONSTRAINT questions_question_type_check
CHECK (
    question_type IN (
        'TEXT',
        'NUMBER',
        'SINGLE_CHOICE',
        'MULTIPLE_CHOICE',
        'SCALE',
        'MATRIX_SCALE',
        'BOOLEAN'
    )
);

COMMIT;