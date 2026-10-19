-- Permite asignar hasta diez etiquetas a una encuesta.

ALTER TABLE surveys DROP CONSTRAINT IF EXISTS ck_surveys_values;

ALTER TABLE surveys
    ADD CONSTRAINT ck_surveys_values CHECK (
        lifecycle_status IN ('DRAFT', 'OPEN', 'CLOSED')
        AND access_mode IN ('PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED')
        AND visibility IN ('LISTED', 'UNLISTED')
        AND font_family IN ('OPEN_DYSLEXIC', 'TIMES_NEW_ROMAN', 'LEXEND', 'ARIAL')
        AND (lifecycle_status <> 'DRAFT' OR visibility = 'UNLISTED')
        AND (
            (access_mode = 'PASSWORD_PROTECTED' AND access_password_hash IS NOT NULL)
            OR (access_mode <> 'PASSWORD_PROTECTED' AND access_password_hash IS NULL)
        )
        AND (public_code IS NULL OR char_length(public_code) = 12)
        AND (lifecycle_status <> 'OPEN' OR public_code IS NOT NULL)
        AND cardinality(tags) <= 10
    );
