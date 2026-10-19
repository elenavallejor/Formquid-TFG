-- Centraliza la tipografía en cada encuesta y retira las opciones visuales de las preguntas.

ALTER TABLE question_settings DROP CONSTRAINT IF EXISTS ck_question_settings_values;

ALTER TABLE question_settings
    DROP COLUMN button_shape,
    DROP COLUMN font_family,
    DROP COLUMN font_size,
    DROP COLUMN text_color;

ALTER TABLE question_settings
    ADD CONSTRAINT ck_question_settings_values CHECK (
        (max_length IS NULL OR max_length > 0)
        AND (step IS NULL OR step > 0)
        AND (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
        AND (min_selections IS NULL OR min_selections > 0)
        AND (max_selections IS NULL OR max_selections > 0)
        AND (min_selections IS NULL OR max_selections IS NULL OR min_selections <= max_selections)
        AND (true_evaluation_factor IS NULL OR true_evaluation_factor BETWEEN 0 AND 1)
        AND (false_evaluation_factor IS NULL OR false_evaluation_factor BETWEEN 0 AND 1)
        AND (scale_display_type IS NULL OR scale_display_type IN ('NUMERIC', 'STARS', 'FACES'))
    );

ALTER TABLE surveys DROP CONSTRAINT IF EXISTS ck_surveys_values;

ALTER TABLE surveys
    ADD COLUMN font_family varchar(30) NOT NULL DEFAULT 'TIMES_NEW_ROMAN';

ALTER TABLE surveys
    ADD CONSTRAINT ck_surveys_values CHECK (
        lifecycle_status IN ('DRAFT', 'OPEN', 'CLOSED')
        AND access_mode IN ('PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED')
        AND visibility IN ('LISTED', 'UNLISTED')
        AND font_family IN ('OPEN_DYSLEXIC', 'TIMES_NEW_ROMAN', 'LEXEND', 'ARIAL')
        AND (
            (access_mode = 'PASSWORD_PROTECTED' AND access_password_hash IS NOT NULL)
            OR (access_mode <> 'PASSWORD_PROTECTED' AND access_password_hash IS NULL)
        )
        AND (public_code IS NULL OR char_length(public_code) = 12)
        AND (lifecycle_status <> 'OPEN' OR public_code IS NOT NULL)
        AND cardinality(tags) <= 4
    );
