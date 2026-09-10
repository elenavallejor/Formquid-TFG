BEGIN;

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT constraint_data.conname
        FROM pg_constraint AS constraint_data
        JOIN pg_class AS table_data
            ON table_data.oid = constraint_data.conrelid
        JOIN pg_namespace AS schema_data
            ON schema_data.oid = table_data.relnamespace
        WHERE schema_data.nspname = 'public'
          AND table_data.relname = 'questions'
          AND constraint_data.contype = 'c'
          AND pg_get_constraintdef(constraint_data.oid) ILIKE '%catalog_name%'
    LOOP
        EXECUTE format(
            'ALTER TABLE public.questions DROP CONSTRAINT %I',
            constraint_name
        );
    END LOOP;
END $$;

ALTER TABLE public.questions
    DROP COLUMN IF EXISTS catalog_name;

ALTER TABLE public.questions
    DROP CONSTRAINT IF EXISTS questions_consistency_without_catalog_name_check;

ALTER TABLE public.questions
    ADD CONSTRAINT questions_consistency_without_catalog_name_check CHECK (
        id ~ '^Q[1-9][0-9]*$'
        AND scope IN ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
        AND question_type IN (
            'TEXT',
            'NUMBER',
            'SINGLE_CHOICE',
            'MULTIPLE_CHOICE',
            'SCALE',
            'MATRIX_SCALE',
            'BOOLEAN'
        )
        AND evaluation_direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
        AND (
            (
                scope = 'CATALOG_FIXED'
                AND parent_item_id IS NULL
                AND source_fixed_question_id IS NULL
            )
            OR (scope = 'LOCAL' AND parent_item_id IS NOT NULL)
            OR (scope = 'FIXED_SNAPSHOT' AND parent_item_id IS NOT NULL)
        )
        AND (
            (
                evaluation_direction = 'NEUTRAL'
                AND evaluation_weight IS NULL
            )
            OR (
                evaluation_direction IN ('POSITIVE', 'NEGATIVE')
                AND evaluation_weight > 0
            )
        )
    );

COMMIT;