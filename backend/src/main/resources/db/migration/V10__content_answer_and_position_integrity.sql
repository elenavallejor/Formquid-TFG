-- Corrige el orden del contenido, valida las respuestas y protege las preguntas ya respondidas.

WITH RECURSIVE block_ancestry AS (
    SELECT
        block.id AS descendant_id,
        block.source_block_id AS ancestor_id,
        1 AS depth,
        ARRAY[block.id, block.source_block_id]::bigint[] AS path
    FROM blocks block
    WHERE block.source_block_id IS NOT NULL

    UNION ALL

    SELECT
        ancestry.descendant_id,
        parent.source_block_id,
        ancestry.depth + 1,
        ancestry.path || parent.source_block_id
    FROM block_ancestry ancestry
    JOIN blocks parent ON parent.id = ancestry.ancestor_id
    WHERE parent.source_block_id IS NOT NULL
      AND NOT parent.source_block_id = ANY(ancestry.path)
), catalog_sources AS (
    SELECT DISTINCT ON (ancestry.descendant_id)
        ancestry.descendant_id,
        ancestry.ancestor_id AS catalog_id
    FROM block_ancestry ancestry
    JOIN blocks source ON source.id = ancestry.ancestor_id
    WHERE source.scope = 'CATALOG'
    ORDER BY ancestry.descendant_id, ancestry.depth
)
UPDATE blocks block
SET source_block_id = catalog_sources.catalog_id
FROM catalog_sources
WHERE block.id = catalog_sources.descendant_id
  AND block.source_block_id IS DISTINCT FROM catalog_sources.catalog_id;

UPDATE blocks block
SET source_block_id = NULL
WHERE block.scope = 'CATALOG'
   OR (
       block.source_block_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM blocks source
           WHERE source.id = block.source_block_id
             AND source.scope = 'CATALOG'
       )
   );

WITH RECURSIVE question_ancestry AS (
    SELECT
        question.id AS descendant_id,
        question.source_fixed_question_id AS ancestor_id,
        1 AS depth,
        ARRAY[question.id, question.source_fixed_question_id]::bigint[] AS path
    FROM questions question
    WHERE question.source_fixed_question_id IS NOT NULL

    UNION ALL

    SELECT
        ancestry.descendant_id,
        parent.source_fixed_question_id,
        ancestry.depth + 1,
        ancestry.path || parent.source_fixed_question_id
    FROM question_ancestry ancestry
    JOIN questions parent ON parent.id = ancestry.ancestor_id
    WHERE parent.source_fixed_question_id IS NOT NULL
      AND NOT parent.source_fixed_question_id = ANY(ancestry.path)
), catalog_sources AS (
    SELECT DISTINCT ON (ancestry.descendant_id)
        ancestry.descendant_id,
        ancestry.ancestor_id AS catalog_id
    FROM question_ancestry ancestry
    JOIN questions source ON source.id = ancestry.ancestor_id
    WHERE source.scope = 'CATALOG_FIXED'
    ORDER BY ancestry.descendant_id, ancestry.depth
)
UPDATE questions question
SET source_fixed_question_id = catalog_sources.catalog_id
FROM catalog_sources
WHERE question.id = catalog_sources.descendant_id
  AND question.source_fixed_question_id IS DISTINCT FROM catalog_sources.catalog_id;

UPDATE questions
SET source_fixed_question_id = NULL
WHERE scope IN ('CATALOG_FIXED', 'LOCAL');

UPDATE questions question
SET scope = 'LOCAL', source_fixed_question_id = NULL
WHERE question.scope = 'FIXED_SNAPSHOT'
  AND NOT EXISTS (
      SELECT 1
      FROM questions source
      WHERE source.id = question.source_fixed_question_id
        AND source.scope = 'CATALOG_FIXED'
  );

ALTER TABLE blocks DROP CONSTRAINT IF EXISTS ck_blocks_values;
ALTER TABLE blocks
    ADD CONSTRAINT ck_blocks_values CHECK (
        scope IN ('CATALOG', 'SNAPSHOT')
        AND (
            (scope = 'CATALOG' AND parent_item_id IS NULL AND source_block_id IS NULL)
            OR (scope = 'SNAPSHOT' AND parent_item_id IS NOT NULL)
        )
    );

ALTER TABLE questions DROP CONSTRAINT IF EXISTS ck_questions_values;
ALTER TABLE questions
    ADD CONSTRAINT ck_questions_values CHECK (
        scope IN ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
        AND question_type IN ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN')
        AND evaluation_direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
        AND (
            (scope = 'CATALOG_FIXED' AND parent_item_id IS NULL AND source_fixed_question_id IS NULL)
            OR (scope = 'LOCAL' AND parent_item_id IS NOT NULL AND source_fixed_question_id IS NULL)
            OR (scope = 'FIXED_SNAPSHOT' AND parent_item_id IS NOT NULL AND source_fixed_question_id IS NOT NULL)
        )
        AND (
            (evaluation_direction = 'NEUTRAL' AND evaluation_weight IS NULL)
            OR (evaluation_direction IN ('POSITIVE', 'NEGATIVE') AND evaluation_weight > 0)
        )
    );

ALTER TABLE question_settings DROP CONSTRAINT IF EXISTS ck_question_settings_values;
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
        AND (font_size IS NULL OR font_size > 0)
        AND (text_color IS NULL OR text_color ~ '^#[0-9A-Fa-f]{6}$')
        AND (button_shape IS NULL OR button_shape IN ('RECTANGULAR', 'ROUNDED', 'PILL'))
        AND (scale_display_type IS NULL OR scale_display_type IN ('NUMERIC', 'STARS', 'FACES'))
    );

ALTER TABLE blocks DROP CONSTRAINT IF EXISTS fk_blocks_source;
ALTER TABLE blocks
    ADD CONSTRAINT fk_blocks_source
    FOREIGN KEY (source_block_id) REFERENCES blocks(id) ON DELETE RESTRICT;

ALTER TABLE questions DROP CONSTRAINT IF EXISTS fk_questions_source_fixed;
ALTER TABLE questions
    ADD CONSTRAINT fk_questions_source_fixed
    FOREIGN KEY (source_fixed_question_id) REFERENCES questions(id) ON DELETE RESTRICT;

CREATE OR REPLACE FUNCTION validate_catalog_source_reference()
RETURNS trigger AS $$
BEGIN
    IF TG_TABLE_NAME = 'blocks' THEN
        IF NEW.source_block_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM blocks source
            WHERE source.id = NEW.source_block_id AND source.scope = 'CATALOG'
        ) THEN
            RAISE EXCEPTION 'El bloque origen debe pertenecer al catálogo';
        END IF;
        IF NEW.scope <> 'CATALOG' AND EXISTS (
            SELECT 1 FROM blocks copy WHERE copy.source_block_id = NEW.id
        ) THEN
            RAISE EXCEPTION 'Un bloque referenciado como origen debe permanecer en el catálogo';
        END IF;
    ELSE
        IF NEW.source_fixed_question_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM questions source
            WHERE source.id = NEW.source_fixed_question_id AND source.scope = 'CATALOG_FIXED'
        ) THEN
            RAISE EXCEPTION 'La pregunta origen debe pertenecer al catálogo fijo';
        END IF;
        IF NEW.scope <> 'CATALOG_FIXED' AND EXISTS (
            SELECT 1 FROM questions copy WHERE copy.source_fixed_question_id = NEW.id
        ) THEN
            RAISE EXCEPTION 'Una pregunta referenciada como origen debe permanecer en el catálogo fijo';
        END IF;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_blocks_catalog_source
AFTER INSERT OR UPDATE ON blocks
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_catalog_source_reference();

CREATE CONSTRAINT TRIGGER trg_questions_catalog_source
AFTER INSERT OR UPDATE ON questions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_catalog_source_reference();

DROP INDEX IF EXISTS ux_content_items_survey_position;
DROP INDEX IF EXISTS ux_content_items_template_position;
DROP INDEX IF EXISTS ux_content_items_block_position;

WITH normalized AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY survey_id, template_id, parent_block_id
            ORDER BY position, id
        )::integer AS normalized_position
    FROM content_items
)
UPDATE content_items item
SET position = normalized.normalized_position
FROM normalized
WHERE item.id = normalized.id
  AND item.position IS DISTINCT FROM normalized.normalized_position;

CREATE UNIQUE INDEX ux_content_items_survey_position
    ON content_items(survey_id, position)
    WHERE survey_id IS NOT NULL;
CREATE UNIQUE INDEX ux_content_items_template_position
    ON content_items(template_id, position)
    WHERE template_id IS NOT NULL;
CREATE UNIQUE INDEX ux_content_items_block_position
    ON content_items(parent_block_id, position)
    WHERE parent_block_id IS NOT NULL;

ALTER TABLE question_options DROP CONSTRAINT IF EXISTS uk_question_options_position;
WITH normalized AS (
    SELECT
        id,
        row_number() OVER (PARTITION BY question_id ORDER BY position, id)::integer AS normalized_position
    FROM question_options
)
UPDATE question_options option_value
SET position = normalized.normalized_position
FROM normalized
WHERE option_value.id = normalized.id
  AND option_value.position IS DISTINCT FROM normalized.normalized_position;
ALTER TABLE question_options
    ADD CONSTRAINT uk_question_options_position UNIQUE (question_id, position);

ALTER TABLE matrix_rows DROP CONSTRAINT IF EXISTS uk_matrix_rows_position;
WITH normalized AS (
    SELECT
        id,
        row_number() OVER (PARTITION BY question_id ORDER BY position, id)::integer AS normalized_position
    FROM matrix_rows
)
UPDATE matrix_rows row_value
SET position = normalized.normalized_position
FROM normalized
WHERE row_value.id = normalized.id
  AND row_value.position IS DISTINCT FROM normalized.normalized_position;
ALTER TABLE matrix_rows
    ADD CONSTRAINT uk_matrix_rows_position UNIQUE (question_id, position);

CREATE OR REPLACE FUNCTION validate_content_positions(
    checked_survey_id bigint,
    checked_template_id bigint,
    checked_block_id bigint
)
RETURNS void AS $$
DECLARE
    item_count bigint;
    minimum_position integer;
    maximum_position integer;
BEGIN
    IF checked_survey_id IS NOT NULL THEN
        SELECT count(*), min(position), max(position)
        INTO item_count, minimum_position, maximum_position
        FROM content_items WHERE survey_id = checked_survey_id;
    ELSIF checked_template_id IS NOT NULL THEN
        SELECT count(*), min(position), max(position)
        INTO item_count, minimum_position, maximum_position
        FROM content_items WHERE template_id = checked_template_id;
    ELSIF checked_block_id IS NOT NULL THEN
        SELECT count(*), min(position), max(position)
        INTO item_count, minimum_position, maximum_position
        FROM content_items WHERE parent_block_id = checked_block_id;
    ELSE
        RETURN;
    END IF;

    IF item_count > 0 AND (minimum_position <> 1 OR maximum_position <> item_count) THEN
        RAISE EXCEPTION 'Las posiciones de contenido deben ser consecutivas desde 1';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION content_position_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM validate_content_positions(OLD.survey_id, OLD.template_id, OLD.parent_block_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM validate_content_positions(NEW.survey_id, NEW.template_id, NEW.parent_block_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_content_items_consecutive_positions
AFTER INSERT OR UPDATE OR DELETE ON content_items
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION content_position_constraint_trigger();

CREATE OR REPLACE FUNCTION validate_question_option_positions(checked_question_id bigint)
RETURNS void AS $$
DECLARE
    item_count bigint;
    minimum_position integer;
    maximum_position integer;
BEGIN
    IF checked_question_id IS NULL THEN
        RETURN;
    END IF;
    SELECT count(*), min(position), max(position)
    INTO item_count, minimum_position, maximum_position
    FROM question_options WHERE question_id = checked_question_id;
    IF item_count > 0 AND (minimum_position <> 1 OR maximum_position <> item_count) THEN
        RAISE EXCEPTION 'Las posiciones de opciones deben ser consecutivas desde 1';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION question_option_position_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM validate_question_option_positions(OLD.question_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM validate_question_option_positions(NEW.question_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_question_options_consecutive_positions
AFTER INSERT OR UPDATE OR DELETE ON question_options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION question_option_position_constraint_trigger();

CREATE OR REPLACE FUNCTION validate_matrix_row_positions(checked_question_id bigint)
RETURNS void AS $$
DECLARE
    item_count bigint;
    minimum_position integer;
    maximum_position integer;
BEGIN
    IF checked_question_id IS NULL THEN
        RETURN;
    END IF;
    SELECT count(*), min(position), max(position)
    INTO item_count, minimum_position, maximum_position
    FROM matrix_rows WHERE question_id = checked_question_id;
    IF item_count > 0 AND (minimum_position <> 1 OR maximum_position <> item_count) THEN
        RAISE EXCEPTION 'Las posiciones de filas de matriz deben ser consecutivas desde 1';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION matrix_row_position_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM validate_matrix_row_positions(OLD.question_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM validate_matrix_row_positions(NEW.question_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_matrix_rows_consecutive_positions
AFTER INSERT OR UPDATE OR DELETE ON matrix_rows
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION matrix_row_position_constraint_trigger();

CREATE OR REPLACE FUNCTION validate_content_item_subtype(checked_item_id bigint)
RETURNS void AS $$
DECLARE
    checked_type varchar(30);
    question_count bigint;
    block_count bigint;
BEGIN
    SELECT item_type INTO checked_type
    FROM content_items WHERE id = checked_item_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    SELECT count(*) INTO question_count FROM questions WHERE parent_item_id = checked_item_id;
    SELECT count(*) INTO block_count FROM blocks WHERE parent_item_id = checked_item_id;

    IF checked_type = 'QUESTION' AND (question_count <> 1 OR block_count <> 0) THEN
        RAISE EXCEPTION 'El elemento QUESTION debe tener exactamente una pregunta asociada';
    ELSIF checked_type = 'BLOCK' AND (block_count <> 1 OR question_count <> 0) THEN
        RAISE EXCEPTION 'El elemento BLOCK debe tener exactamente un bloque asociado';
    ELSIF checked_type = 'TEXT' AND (question_count <> 0 OR block_count <> 0) THEN
        RAISE EXCEPTION 'El elemento TEXT no puede tener pregunta ni bloque asociados';
    END IF;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    checked_item record;
BEGIN
    FOR checked_item IN SELECT id FROM content_items LOOP
        PERFORM validate_content_item_subtype(checked_item.id);
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION content_item_subtype_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM validate_content_item_subtype(OLD.id);
    ELSE
        PERFORM validate_content_item_subtype(NEW.id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION subtype_parent_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') AND OLD.parent_item_id IS NOT NULL THEN
        PERFORM validate_content_item_subtype(OLD.parent_item_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.parent_item_id IS NOT NULL THEN
        PERFORM validate_content_item_subtype(NEW.parent_item_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_content_items_subtype
AFTER INSERT OR UPDATE OR DELETE ON content_items
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION content_item_subtype_constraint_trigger();

CREATE CONSTRAINT TRIGGER trg_questions_parent_subtype
AFTER INSERT OR UPDATE OR DELETE ON questions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION subtype_parent_constraint_trigger();

CREATE CONSTRAINT TRIGGER trg_blocks_parent_subtype
AFTER INSERT OR UPDATE OR DELETE ON blocks
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION subtype_parent_constraint_trigger();

CREATE OR REPLACE FUNCTION resolved_question_survey_id(checked_question_id bigint)
RETURNS bigint AS $$
    SELECT coalesce(question_item.survey_id, block_parent_item.survey_id)
    FROM questions question
    JOIN content_items question_item ON question_item.id = question.parent_item_id
    LEFT JOIN blocks parent_block ON parent_block.id = question_item.parent_block_id
    LEFT JOIN content_items block_parent_item ON block_parent_item.id = parent_block.parent_item_id
    WHERE question.id = checked_question_id
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION validate_answer_integrity(checked_answer_id bigint)
RETURNS void AS $$
DECLARE
    checked_question_id bigint;
    participation_survey_id bigint;
    question_survey_id bigint;
    checked_question_type varchar(40);
    checked_text_value text;
    checked_numeric_value numeric;
    checked_boolean_value boolean;
    configured_max_length integer;
    configured_min_value numeric;
    configured_max_value numeric;
    configured_step numeric;
    configured_min_selections integer;
    configured_max_selections integer;
    selected_option_count bigint;
    matrix_value_count bigint;
    matrix_row_count bigint;
BEGIN
    SELECT question_id INTO checked_question_id
    FROM answers
    WHERE id = checked_answer_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended('formquid:answer-question:' || checked_question_id, 0)
    );

    SELECT
        participation.survey_id,
        question.question_type,
        answer.text_value,
        answer.numeric_value,
        answer.boolean_value,
        settings.max_length,
        settings.min_value,
        settings.max_value,
        settings.step,
        settings.min_selections,
        settings.max_selections
    INTO
        participation_survey_id,
        checked_question_type,
        checked_text_value,
        checked_numeric_value,
        checked_boolean_value,
        configured_max_length,
        configured_min_value,
        configured_max_value,
        configured_step,
        configured_min_selections,
        configured_max_selections
    FROM answers answer
    JOIN participations participation ON participation.id = answer.participation_id
    JOIN questions question ON question.id = answer.question_id
    LEFT JOIN question_settings settings ON settings.question_id = question.id
    WHERE answer.id = checked_answer_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    question_survey_id := resolved_question_survey_id(checked_question_id);
    IF question_survey_id IS NULL OR question_survey_id <> participation_survey_id THEN
        RAISE EXCEPTION 'La pregunta respondida no pertenece a la encuesta de la participación';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM answer_options selected
        JOIN question_options option_value ON option_value.id = selected.option_id
        WHERE selected.answer_id = checked_answer_id
          AND option_value.question_id <> checked_question_id
    ) THEN
        RAISE EXCEPTION 'La opción seleccionada no pertenece a la pregunta respondida';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM matrix_answer_values matrix_value
        JOIN matrix_rows row_value ON row_value.id = matrix_value.matrix_row_id
        WHERE matrix_value.answer_id = checked_answer_id
          AND row_value.question_id <> checked_question_id
    ) THEN
        RAISE EXCEPTION 'La fila de matriz no pertenece a la pregunta respondida';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM answer_options selected
        JOIN question_options option_value ON option_value.id = selected.option_id
        WHERE selected.answer_id = checked_answer_id
          AND (
              (
                  option_value.is_other
                  AND (
                      selected.other_text IS NULL
                      OR btrim(selected.other_text) = ''
                      OR char_length(selected.other_text) > 500
                  )
              )
              OR (
                  NOT option_value.is_other
                  AND selected.other_text IS NOT NULL
                  AND btrim(selected.other_text) <> ''
              )
          )
    ) THEN
        RAISE EXCEPTION 'El texto adicional no es valido para la opcion seleccionada';
    END IF;

    SELECT count(*) INTO selected_option_count
    FROM answer_options
    WHERE answer_id = checked_answer_id;

    SELECT count(*) INTO matrix_value_count
    FROM matrix_answer_values
    WHERE answer_id = checked_answer_id;

    IF checked_question_type = 'TEXT' THEN
        IF checked_text_value IS NULL
           OR btrim(checked_text_value) = ''
           OR char_length(checked_text_value) > coalesce(configured_max_length, 5000)
           OR checked_numeric_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR selected_option_count <> 0
           OR matrix_value_count <> 0 THEN
            RAISE EXCEPTION 'La respuesta no es valida para una pregunta de texto';
        END IF;
    ELSIF checked_question_type = 'NUMBER' THEN
        IF checked_numeric_value IS NULL
           OR checked_text_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR selected_option_count <> 0
           OR matrix_value_count <> 0
           OR (configured_min_value IS NOT NULL AND checked_numeric_value < configured_min_value)
           OR (configured_max_value IS NOT NULL AND checked_numeric_value > configured_max_value)
           OR (
               configured_step IS NOT NULL
               AND mod(checked_numeric_value - coalesce(configured_min_value, 0), configured_step) <> 0
           ) THEN
            RAISE EXCEPTION 'La respuesta no es valida para una pregunta numerica';
        END IF;
    ELSIF checked_question_type = 'SCALE' THEN
        IF configured_min_value IS NULL
           OR configured_max_value IS NULL
           OR configured_step IS NULL
           OR checked_numeric_value IS NULL
           OR checked_text_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR selected_option_count <> 0
           OR matrix_value_count <> 0
           OR checked_numeric_value < configured_min_value
           OR checked_numeric_value > configured_max_value
           OR mod(checked_numeric_value - configured_min_value, configured_step) <> 0 THEN
            RAISE EXCEPTION 'La respuesta no es valida para una pregunta de escala';
        END IF;
    ELSIF checked_question_type = 'BOOLEAN' THEN
        IF checked_boolean_value IS NULL
           OR checked_text_value IS NOT NULL
           OR checked_numeric_value IS NOT NULL
           OR selected_option_count <> 0
           OR matrix_value_count <> 0 THEN
            RAISE EXCEPTION 'La respuesta no es valida para una pregunta booleana';
        END IF;
    ELSIF checked_question_type = 'SINGLE_CHOICE' THEN
        IF checked_text_value IS NOT NULL
           OR checked_numeric_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR selected_option_count <> 1
           OR matrix_value_count <> 0 THEN
            RAISE EXCEPTION 'La seleccion unica debe contener exactamente una opcion';
        END IF;
    ELSIF checked_question_type = 'MULTIPLE_CHOICE' THEN
        IF checked_text_value IS NOT NULL
           OR checked_numeric_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR matrix_value_count <> 0
           OR selected_option_count < coalesce(configured_min_selections, 1)
           OR (
               configured_max_selections IS NOT NULL
               AND selected_option_count > configured_max_selections
           ) THEN
            RAISE EXCEPTION 'La respuesta no respeta los limites de seleccion multiple';
        END IF;
    ELSIF checked_question_type = 'MATRIX_SCALE' THEN
        SELECT count(*) INTO matrix_row_count
        FROM matrix_rows
        WHERE question_id = checked_question_id;

        IF configured_min_value IS NULL
           OR configured_max_value IS NULL
           OR configured_step IS NULL
           OR checked_text_value IS NOT NULL
           OR checked_numeric_value IS NOT NULL
           OR checked_boolean_value IS NOT NULL
           OR selected_option_count <> 0
           OR matrix_row_count = 0
           OR matrix_value_count <> matrix_row_count
           OR EXISTS (
               SELECT 1
               FROM matrix_answer_values matrix_value
               WHERE matrix_value.answer_id = checked_answer_id
                 AND (
                     matrix_value.numeric_value < configured_min_value
                     OR matrix_value.numeric_value > configured_max_value
                     OR mod(matrix_value.numeric_value - configured_min_value, configured_step) <> 0
                 )
           ) THEN
            RAISE EXCEPTION 'La respuesta matricial no contiene todas las filas o incluye valores no validos';
        END IF;
    ELSE
        RAISE EXCEPTION 'El tipo de la pregunta respondida no es valido';
    END IF;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    checked_answer record;
BEGIN
    FOR checked_answer IN SELECT id FROM answers LOOP
        PERFORM validate_answer_integrity(checked_answer.id);
    END LOOP;
END;
$$;

CREATE OR REPLACE FUNCTION answer_integrity_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM validate_answer_integrity(OLD.id);
    ELSE
        PERFORM validate_answer_integrity(NEW.id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION answer_child_integrity_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM validate_answer_integrity(OLD.answer_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM validate_answer_integrity(NEW.answer_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_answers_integrity
AFTER INSERT OR UPDATE OR DELETE ON answers
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION answer_integrity_constraint_trigger();

CREATE CONSTRAINT TRIGGER trg_answer_options_integrity
AFTER INSERT OR UPDATE OR DELETE ON answer_options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION answer_child_integrity_constraint_trigger();

CREATE CONSTRAINT TRIGGER trg_matrix_answer_values_integrity
AFTER INSERT OR UPDATE OR DELETE ON matrix_answer_values
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION answer_child_integrity_constraint_trigger();

CREATE INDEX IF NOT EXISTS idx_answer_options_option
    ON answer_options(option_id);
CREATE INDEX IF NOT EXISTS idx_matrix_answer_values_row
    ON matrix_answer_values(matrix_row_id);

CREATE OR REPLACE FUNCTION prevent_answered_question_change(checked_question_id bigint)
RETURNS void AS $$
BEGIN
    IF checked_question_id IS NULL THEN
        RETURN;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended('formquid:answer-question:' || checked_question_id, 0)
    );

    IF EXISTS (SELECT 1 FROM answers WHERE question_id = checked_question_id) THEN
        RAISE EXCEPTION 'No se puede modificar una pregunta que ya tiene respuestas';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION answer_question_structure_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM prevent_answered_question_change(OLD.id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM prevent_answered_question_change(NEW.id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_questions_answer_integrity
AFTER INSERT OR UPDATE OR DELETE ON questions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION answer_question_structure_constraint_trigger();

CREATE OR REPLACE FUNCTION question_settings_answer_integrity_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM prevent_answered_question_change(OLD.question_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM prevent_answered_question_change(NEW.question_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_question_settings_answer_integrity
AFTER INSERT OR UPDATE OR DELETE ON question_settings
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION question_settings_answer_integrity_constraint_trigger();

CREATE OR REPLACE FUNCTION prevent_participation_survey_change()
RETURNS trigger AS $$
BEGIN
    IF NEW.survey_id IS DISTINCT FROM OLD.survey_id THEN
        RAISE EXCEPTION 'No se puede cambiar la encuesta de una participacion existente';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_participations_immutable_survey
BEFORE UPDATE OF survey_id ON participations
FOR EACH ROW EXECUTE FUNCTION prevent_participation_survey_change();

CREATE OR REPLACE FUNCTION participation_answer_integrity_constraint_trigger()
RETURNS trigger AS $$
DECLARE
    checked_answer record;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RETURN NULL;
    END IF;
    FOR checked_answer IN SELECT id FROM answers WHERE participation_id = NEW.id LOOP
        PERFORM validate_answer_integrity(checked_answer.id);
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_participations_answer_integrity
AFTER INSERT OR UPDATE ON participations
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION participation_answer_integrity_constraint_trigger();

CREATE OR REPLACE FUNCTION option_answer_integrity_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM prevent_answered_question_change(OLD.question_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM prevent_answered_question_change(NEW.question_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_question_options_answer_integrity
AFTER INSERT OR UPDATE OR DELETE ON question_options
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION option_answer_integrity_constraint_trigger();

CREATE OR REPLACE FUNCTION row_answer_integrity_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM prevent_answered_question_change(OLD.question_id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM prevent_answered_question_change(NEW.question_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_matrix_rows_answer_integrity
AFTER INSERT OR UPDATE OR DELETE ON matrix_rows
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION row_answer_integrity_constraint_trigger();

CREATE OR REPLACE FUNCTION validate_answers_for_content_item(checked_item_id bigint)
RETURNS void AS $$
DECLARE
    checked_question record;
BEGIN
    FOR checked_question IN
        SELECT question.id
        FROM questions question
        WHERE question.parent_item_id = checked_item_id

        UNION

        SELECT question.id
        FROM blocks block
        JOIN content_items nested_item ON nested_item.parent_block_id = block.id
        JOIN questions question ON question.parent_item_id = nested_item.id
        WHERE block.parent_item_id = checked_item_id
    LOOP
        PERFORM prevent_answered_question_change(checked_question.id);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION content_answer_structure_constraint_trigger()
RETURNS trigger AS $$
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        PERFORM validate_answers_for_content_item(OLD.id);
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        PERFORM validate_answers_for_content_item(NEW.id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_content_items_answer_integrity
AFTER INSERT OR UPDATE OR DELETE ON content_items
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION content_answer_structure_constraint_trigger();

CREATE OR REPLACE FUNCTION block_answer_structure_constraint_trigger()
RETURNS trigger AS $$
DECLARE
    checked_question record;
BEGIN
    IF TG_OP = 'DELETE' THEN
        RETURN NULL;
    END IF;
    FOR checked_question IN
        SELECT question.id
        FROM content_items nested_item
        JOIN questions question ON question.parent_item_id = nested_item.id
        WHERE nested_item.parent_block_id = NEW.id
    LOOP
        PERFORM prevent_answered_question_change(checked_question.id);
    END LOOP;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_blocks_answer_integrity
AFTER INSERT OR UPDATE ON blocks
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION block_answer_structure_constraint_trigger();
