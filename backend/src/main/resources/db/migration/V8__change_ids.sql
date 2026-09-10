SET lock_timeout = '30s';

ALTER TABLE registration_requests
    ADD COLUMN IF NOT EXISTS motivation varchar(1000);

UPDATE registration_requests
SET motivation = 'Sin motivación especificada'
WHERE motivation IS NULL;

ALTER TABLE surveys
    ADD COLUMN IF NOT EXISTS public_code varchar(12);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'registration_requests'
          AND column_name = 'status'
    ) THEN
        EXECUTE 'DELETE FROM registration_requests WHERE status <> ''PENDING''';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'collaboration_requests'
          AND column_name = 'status'
    ) THEN
        EXECUTE 'DELETE FROM collaboration_requests WHERE status <> ''PENDING''';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'publication_requests'
          AND column_name = 'status'
    ) THEN
        EXECUTE 'DELETE FROM publication_requests WHERE status <> ''PENDING''';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'element_requests'
          AND column_name = 'status'
    ) THEN
        EXECUTE 'DELETE FROM element_requests WHERE status <> ''PENDING''';
    END IF;
END;
$$;

CREATE TEMPORARY TABLE map_users (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_users (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM users;

CREATE TEMPORARY TABLE map_registration_requests (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_registration_requests (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM registration_requests;

CREATE TEMPORARY TABLE map_templates (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_templates (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM templates;

CREATE TEMPORARY TABLE map_surveys (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_surveys (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM surveys;

CREATE TEMPORARY TABLE map_collaboration_requests (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_collaboration_requests (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM collaboration_requests;

CREATE TEMPORARY TABLE map_publication_requests (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_publication_requests (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM publication_requests;

CREATE TEMPORARY TABLE map_content_items (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_content_items (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM content_items;

CREATE TEMPORARY TABLE map_blocks (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_blocks (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM blocks;

CREATE TEMPORARY TABLE map_questions (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_questions (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM questions;

CREATE TEMPORARY TABLE map_question_options (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_question_options (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM question_options;

CREATE TEMPORARY TABLE map_matrix_rows (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_matrix_rows (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM matrix_rows;

CREATE TEMPORARY TABLE map_element_requests (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_element_requests (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM element_requests;

CREATE TEMPORARY TABLE map_participations (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_participations (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM participations;

CREATE TEMPORARY TABLE map_answers (
    old_id text PRIMARY KEY,
    new_id bigint UNIQUE NOT NULL
) ON COMMIT DROP;

INSERT INTO map_answers (old_id, new_id)
SELECT id::text, row_number() OVER (ORDER BY id::text)::bigint
FROM answers;

CREATE TABLE users_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    role varchar(30) NOT NULL,
    email varchar(320) NOT NULL,
    name varchar(150) NOT NULL,
    gender varchar(20) NOT NULL,
    institution varchar(200) NOT NULL,
    password_hash varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT ck_users_values CHECK (
        role IN ('EDITOR', 'ADMIN', 'SUPERADMIN')
        AND gender IN ('MALE', 'FEMALE', 'OTHER')
        AND email = lower(btrim(email))
    ),
    CONSTRAINT tmp_uk_users_email UNIQUE (email)
);

CREATE TABLE registration_requests_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    email varchar(320) NOT NULL,
    name varchar(150) NOT NULL,
    gender varchar(20) NOT NULL,
    institution varchar(200) NOT NULL,
    motivation varchar(1000) NOT NULL,
    password_hash varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_registration_requests_values CHECK (
        gender IN ('MALE', 'FEMALE', 'OTHER')
        AND email = lower(btrim(email))
    )
);

CREATE TABLE templates_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    title varchar(200) NOT NULL,
    description text,
    image_key text,
    created_by_user_id bigint,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE surveys_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    public_code varchar(12),
    owner_id bigint NOT NULL,
    title varchar(200) NOT NULL,
    description text,
    image_key text,
    lifecycle_status varchar(30) NOT NULL,
    access_mode varchar(40) NOT NULL,
    visibility varchar(30) NOT NULL,
    access_password_hash varchar(255),
    tags varchar(50)[] NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT tmp_uk_surveys_public_code UNIQUE (public_code),
    CONSTRAINT ck_surveys_values CHECK (
        lifecycle_status IN ('DRAFT', 'OPEN', 'CLOSED')
        AND access_mode IN ('PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED')
        AND visibility IN ('LISTED', 'UNLISTED')
        AND (
            (access_mode = 'PASSWORD_PROTECTED' AND access_password_hash IS NOT NULL)
            OR (access_mode <> 'PASSWORD_PROTECTED' AND access_password_hash IS NULL)
        )
        AND (public_code IS NULL OR char_length(public_code) = 12)
        AND (lifecycle_status <> 'OPEN' OR public_code IS NOT NULL)
        AND cardinality(tags) <= 4
    )
);

CREATE TABLE survey_collaborators_bigint (
    survey_id bigint NOT NULL,
    user_id bigint NOT NULL,
    joined_at timestamptz NOT NULL,
    PRIMARY KEY (survey_id, user_id)
);

CREATE TABLE collaboration_requests_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    survey_id bigint NOT NULL,
    user_id bigint NOT NULL,
    request_type varchar(30) NOT NULL,
    message text,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_collaboration_requests_type CHECK (
        request_type IN ('INVITATION', 'ACCESS_REQUEST')
    )
);

CREATE TABLE publication_requests_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    survey_id bigint NOT NULL,
    requested_by_user_id bigint NOT NULL,
    message text,
    created_at timestamptz NOT NULL
);

CREATE TABLE content_items_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    survey_id bigint,
    template_id bigint,
    parent_block_id bigint,
    item_type varchar(30) NOT NULL,
    position integer NOT NULL,
    is_required boolean,
    text_body text,
    CONSTRAINT ck_content_items_values CHECK (
        item_type IN ('QUESTION', 'BLOCK', 'TEXT')
        AND position >= 1
        AND (
            (survey_id IS NOT NULL)::integer
            + (template_id IS NOT NULL)::integer
            + (parent_block_id IS NOT NULL)::integer
        ) = 1
        AND (
            (item_type = 'QUESTION' AND is_required IS NOT NULL AND text_body IS NULL)
            OR (item_type = 'BLOCK' AND is_required IS NULL AND text_body IS NULL)
            OR (item_type = 'TEXT' AND is_required IS NULL AND text_body IS NOT NULL)
        )
    )
);

CREATE TABLE blocks_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    parent_item_id bigint,
    scope varchar(30) NOT NULL,
    source_block_id bigint,
    name varchar(150) NOT NULL,
    description text,
    created_by_user_id bigint,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT tmp_uk_blocks_parent_item UNIQUE (parent_item_id),
    CONSTRAINT ck_blocks_values CHECK (
        scope IN ('CATALOG', 'SNAPSHOT')
        AND (
            (scope = 'CATALOG' AND parent_item_id IS NULL)
            OR (scope = 'SNAPSHOT' AND parent_item_id IS NOT NULL)
        )
    )
);

CREATE TABLE questions_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    parent_item_id bigint,
    scope varchar(30) NOT NULL,
    statement text NOT NULL,
    annotation text,
    question_type varchar(40) NOT NULL,
    evaluation_direction varchar(30) NOT NULL,
    evaluation_weight numeric,
    source_fixed_question_id bigint,
    created_by_user_id bigint,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT tmp_uk_questions_parent_item UNIQUE (parent_item_id),
    CONSTRAINT ck_questions_values CHECK (
        scope IN ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')
        AND question_type IN ('TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BOOLEAN')
        AND evaluation_direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
        AND (
            (scope = 'CATALOG_FIXED' AND parent_item_id IS NULL AND source_fixed_question_id IS NULL)
            OR (scope = 'LOCAL' AND parent_item_id IS NOT NULL)
            OR (scope = 'FIXED_SNAPSHOT' AND parent_item_id IS NOT NULL)
        )
        AND (
            (evaluation_direction = 'NEUTRAL' AND evaluation_weight IS NULL)
            OR (evaluation_direction IN ('POSITIVE', 'NEGATIVE') AND evaluation_weight > 0)
        )
    )
);

CREATE TABLE question_settings_bigint (
    question_id bigint PRIMARY KEY,
    max_length integer,
    min_value numeric,
    max_value numeric,
    step numeric,
    min_selections integer,
    max_selections integer,
    left_label text,
    right_label text,
    true_label varchar(100),
    false_label varchar(100),
    true_evaluation_factor numeric,
    false_evaluation_factor numeric,
    font_family varchar(80),
    font_size smallint,
    text_color varchar(7),
    button_shape varchar(30),
    scale_display_type varchar(30),
    CONSTRAINT ck_question_settings_values CHECK (
        (max_length IS NULL OR max_length > 0)
        AND (step IS NULL OR step > 0)
        AND (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
        AND (min_selections IS NULL OR min_selections > 0)
        AND (max_selections IS NULL OR max_selections > 0)
        AND (min_selections IS NULL OR max_selections IS NULL OR min_selections <= max_selections)
        AND (true_evaluation_factor IS NULL OR true_evaluation_factor BETWEEN 0 AND 1)
        AND (false_evaluation_factor IS NULL OR false_evaluation_factor BETWEEN 0 AND 1)
        AND (text_color IS NULL OR text_color ~ '^#[0-9A-Fa-f]{6}$')
        AND (button_shape IS NULL OR button_shape IN ('RECTANGULAR', 'ROUNDED', 'PILL'))
        AND (scale_display_type IS NULL OR scale_display_type IN ('NUMERIC', 'STARS', 'FACES'))
    )
);

CREATE TABLE question_options_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    question_id bigint NOT NULL,
    label text NOT NULL,
    position integer NOT NULL,
    is_other boolean NOT NULL,
    evaluation_factor numeric,
    CONSTRAINT tmp_uk_question_options_position UNIQUE (question_id, position),
    CONSTRAINT ck_question_options_values CHECK (
        position >= 1
        AND (evaluation_factor IS NULL OR evaluation_factor BETWEEN 0 AND 1)
    )
);

CREATE TABLE matrix_rows_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    question_id bigint NOT NULL,
    label text NOT NULL,
    position integer NOT NULL,
    CONSTRAINT tmp_uk_matrix_rows_position UNIQUE (question_id, position),
    CONSTRAINT ck_matrix_rows_position CHECK (position >= 1)
);

CREATE TABLE element_requests_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    requested_by_user_id bigint NOT NULL,
    element_type varchar(40) NOT NULL,
    message text NOT NULL,
    request_payload jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_element_requests_type CHECK (
        element_type IN ('FIXED_QUESTION', 'BLOCK', 'TEMPLATE')
    )
);

CREATE TABLE participations_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    survey_id bigint NOT NULL,
    submitted_at timestamptz NOT NULL
);

CREATE TABLE answers_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    participation_id bigint NOT NULL,
    question_id bigint NOT NULL,
    text_value text,
    numeric_value numeric,
    boolean_value boolean,
    CONSTRAINT tmp_uk_answers_participation_question UNIQUE (participation_id, question_id),
    CONSTRAINT ck_answers_single_value CHECK (
        (
            (text_value IS NOT NULL)::integer
            + (numeric_value IS NOT NULL)::integer
            + (boolean_value IS NOT NULL)::integer
        ) <= 1
    )
);

CREATE TABLE answer_options_bigint (
    answer_id bigint NOT NULL,
    option_id bigint NOT NULL,
    other_text text,
    PRIMARY KEY (answer_id, option_id)
);

CREATE TABLE matrix_answer_values_bigint (
    answer_id bigint NOT NULL,
    matrix_row_id bigint NOT NULL,
    numeric_value numeric NOT NULL,
    PRIMARY KEY (answer_id, matrix_row_id)
);

CREATE TABLE notifications_bigint (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    recipient_user_id bigint NOT NULL,
    type varchar(50) NOT NULL,
    channel varchar(30) NOT NULL,
    title varchar(180) NOT NULL,
    message varchar(1500) NOT NULL,
    target_type varchar(50) NOT NULL,
    target_id bigint,
    created_at timestamptz NOT NULL,
    read_at timestamptz,
    CONSTRAINT ck_notifications_values CHECK (
        channel IN ('INTERNAL', 'EMAIL')
        AND type IN (
            'GENERAL', 'REGISTRATION_REQUEST', 'REGISTRATION_APPROVED', 'REGISTRATION_REJECTED',
            'ACCOUNT_UPDATED', 'COLLABORATION_INVITATION', 'COLLABORATION_REQUEST',
            'COLLABORATION_ACCEPTED', 'COLLABORATION_REJECTED', 'PUBLICATION_REQUEST',
            'PUBLICATION_ACCEPTED', 'PUBLICATION_REJECTED', 'ELEMENT_REQUEST',
            'ELEMENT_REQUEST_RESOLVED', 'SURVEY_STATUS_CHANGED', 'SURVEY_OWNERSHIP_TRANSFERRED'
        )
        AND target_type IN (
            'SURVEY', 'REGISTRATION_REQUEST', 'COLLABORATION_REQUEST', 'PUBLICATION_REQUEST',
            'ELEMENT_REQUEST', 'USER', 'QUESTION', 'BLOCK', 'TEMPLATE', 'NONE'
        )
    )
);

INSERT INTO users_bigint (
    id, role, email, name, gender, institution, password_hash, created_at, updated_at
)
SELECT
    mapping.new_id,
    source.role,
    source.email,
    source.name,
    source.gender,
    source.institution,
    source.password_hash,
    source.created_at,
    source.updated_at
FROM users source
JOIN map_users mapping ON mapping.old_id = source.id::text;

INSERT INTO registration_requests_bigint (
    id, email, name, gender, institution, motivation, password_hash, created_at
)
SELECT
    mapping.new_id,
    source.email,
    source.name,
    source.gender,
    source.institution,
    source.motivation,
    source.password_hash,
    source.created_at
FROM registration_requests source
JOIN map_registration_requests mapping ON mapping.old_id = source.id::text;

INSERT INTO templates_bigint (
    id, title, description, image_key, created_by_user_id, created_at, updated_at
)
SELECT
    mapping.new_id,
    source.title,
    source.description,
    source.image_key,
    creator.new_id,
    source.created_at,
    source.updated_at
FROM templates source
JOIN map_templates mapping ON mapping.old_id = source.id::text
LEFT JOIN map_users creator ON creator.old_id = source.created_by_user_id::text;

INSERT INTO surveys_bigint (
    id, public_code, owner_id, title, description, image_key, lifecycle_status,
    access_mode, visibility, access_password_hash, tags, created_at, updated_at
)
SELECT
    mapping.new_id,
    CASE
        WHEN source.public_code ~ '^[A-Z0-9]{12}$' THEN source.public_code
        WHEN source.lifecycle_status = 'OPEN' THEN upper(substr(md5('survey:' || source.id::text), 1, 12))
        ELSE NULL
    END,
    owner_mapping.new_id,
    source.title,
    source.description,
    source.image_key,
    source.lifecycle_status,
    source.access_mode,
    source.visibility,
    source.access_password_hash,
    source.tags,
    source.created_at,
    source.updated_at
FROM surveys source
JOIN map_surveys mapping ON mapping.old_id = source.id::text
JOIN map_users owner_mapping ON owner_mapping.old_id = source.owner_id::text;

INSERT INTO survey_collaborators_bigint (survey_id, user_id, joined_at)
SELECT survey_mapping.new_id, user_mapping.new_id, source.joined_at
FROM survey_collaborators source
JOIN map_surveys survey_mapping ON survey_mapping.old_id = source.survey_id::text
JOIN map_users user_mapping ON user_mapping.old_id = source.user_id::text;

INSERT INTO collaboration_requests_bigint (
    id, survey_id, user_id, request_type, message, created_at
)
SELECT
    mapping.new_id,
    survey_mapping.new_id,
    user_mapping.new_id,
    source.request_type,
    source.message,
    source.created_at
FROM collaboration_requests source
JOIN map_collaboration_requests mapping ON mapping.old_id = source.id::text
JOIN map_surveys survey_mapping ON survey_mapping.old_id = source.survey_id::text
JOIN map_users user_mapping ON user_mapping.old_id = source.user_id::text;

INSERT INTO publication_requests_bigint (
    id, survey_id, requested_by_user_id, message, created_at
)
SELECT
    mapping.new_id,
    survey_mapping.new_id,
    user_mapping.new_id,
    source.message,
    source.created_at
FROM publication_requests source
JOIN map_publication_requests mapping ON mapping.old_id = source.id::text
JOIN map_surveys survey_mapping ON survey_mapping.old_id = source.survey_id::text
JOIN map_users user_mapping ON user_mapping.old_id = source.requested_by_user_id::text;

INSERT INTO content_items_bigint (
    id, survey_id, template_id, parent_block_id, item_type, position, is_required, text_body
)
SELECT
    mapping.new_id,
    survey_mapping.new_id,
    template_mapping.new_id,
    block_mapping.new_id,
    source.item_type,
    source.position,
    source.is_required,
    source.text_body
FROM content_items source
JOIN map_content_items mapping ON mapping.old_id = source.id::text
LEFT JOIN map_surveys survey_mapping ON survey_mapping.old_id = source.survey_id::text
LEFT JOIN map_templates template_mapping ON template_mapping.old_id = source.template_id::text
LEFT JOIN map_blocks block_mapping ON block_mapping.old_id = source.parent_block_id::text;

INSERT INTO blocks_bigint (
    id, parent_item_id, scope, source_block_id, name, description,
    created_by_user_id, created_at, updated_at
)
SELECT
    mapping.new_id,
    parent_mapping.new_id,
    source.scope,
    source_mapping.new_id,
    source.name,
    source.description,
    creator_mapping.new_id,
    source.created_at,
    source.updated_at
FROM blocks source
JOIN map_blocks mapping ON mapping.old_id = source.id::text
LEFT JOIN map_content_items parent_mapping ON parent_mapping.old_id = source.parent_item_id::text
LEFT JOIN map_blocks source_mapping ON source_mapping.old_id = source.source_block_id::text
LEFT JOIN map_users creator_mapping ON creator_mapping.old_id = source.created_by_user_id::text;

INSERT INTO questions_bigint (
    id, parent_item_id, scope, statement, annotation, question_type,
    evaluation_direction, evaluation_weight, source_fixed_question_id,
    created_by_user_id, created_at, updated_at
)
SELECT
    mapping.new_id,
    parent_mapping.new_id,
    source.scope,
    source.statement,
    source.annotation,
    CASE
        WHEN source.question_type IN ('SHORT_TEXT', 'LONG_TEXT') THEN 'TEXT'
        WHEN source.question_type = 'BIPOLAR_SCALE' THEN 'SCALE'
        ELSE source.question_type
    END,
    source.evaluation_direction,
    source.evaluation_weight,
    fixed_mapping.new_id,
    creator_mapping.new_id,
    source.created_at,
    source.updated_at
FROM questions source
JOIN map_questions mapping ON mapping.old_id = source.id::text
LEFT JOIN map_content_items parent_mapping ON parent_mapping.old_id = source.parent_item_id::text
LEFT JOIN map_questions fixed_mapping ON fixed_mapping.old_id = source.source_fixed_question_id::text
LEFT JOIN map_users creator_mapping ON creator_mapping.old_id = source.created_by_user_id::text;

INSERT INTO question_settings_bigint (
    question_id, max_length, min_value, max_value, step, min_selections,
    max_selections, left_label, right_label, true_label, false_label,
    true_evaluation_factor, false_evaluation_factor, font_family, font_size,
    text_color, button_shape, scale_display_type
)
SELECT
    question_mapping.new_id,
    source.max_length,
    source.min_value,
    source.max_value,
    source.step,
    source.min_selections,
    source.max_selections,
    source.left_label,
    source.right_label,
    source.true_label,
    source.false_label,
    source.true_evaluation_factor,
    source.false_evaluation_factor,
    source.font_family,
    source.font_size,
    source.text_color,
    source.button_shape,
    source.scale_display_type
FROM question_settings source
JOIN map_questions question_mapping ON question_mapping.old_id = source.question_id::text;

INSERT INTO question_options_bigint (
    id, question_id, label, position, is_other, evaluation_factor
)
SELECT
    mapping.new_id,
    question_mapping.new_id,
    source.label,
    source.position,
    source.is_other,
    source.evaluation_factor
FROM question_options source
JOIN map_question_options mapping ON mapping.old_id = source.id::text
JOIN map_questions question_mapping ON question_mapping.old_id = source.question_id::text;

INSERT INTO matrix_rows_bigint (id, question_id, label, position)
SELECT
    mapping.new_id,
    question_mapping.new_id,
    source.label,
    source.position
FROM matrix_rows source
JOIN map_matrix_rows mapping ON mapping.old_id = source.id::text
JOIN map_questions question_mapping ON question_mapping.old_id = source.question_id::text;

INSERT INTO element_requests_bigint (
    id, requested_by_user_id, element_type, message, request_payload, created_at
)
SELECT
    mapping.new_id,
    user_mapping.new_id,
    source.element_type,
    source.message,
    source.request_payload,
    source.created_at
FROM element_requests source
JOIN map_element_requests mapping ON mapping.old_id = source.id::text
JOIN map_users user_mapping ON user_mapping.old_id = source.requested_by_user_id::text;

INSERT INTO participations_bigint (id, survey_id, submitted_at)
SELECT mapping.new_id, survey_mapping.new_id, source.submitted_at
FROM participations source
JOIN map_participations mapping ON mapping.old_id = source.id::text
JOIN map_surveys survey_mapping ON survey_mapping.old_id = source.survey_id::text;

INSERT INTO answers_bigint (
    id, participation_id, question_id, text_value, numeric_value, boolean_value
)
SELECT
    mapping.new_id,
    participation_mapping.new_id,
    question_mapping.new_id,
    source.text_value,
    source.numeric_value,
    source.boolean_value
FROM answers source
JOIN map_answers mapping ON mapping.old_id = source.id::text
JOIN map_participations participation_mapping ON participation_mapping.old_id = source.participation_id::text
JOIN map_questions question_mapping ON question_mapping.old_id = source.question_id::text;

INSERT INTO answer_options_bigint (answer_id, option_id, other_text)
SELECT answer_mapping.new_id, option_mapping.new_id, source.other_text
FROM answer_options source
JOIN map_answers answer_mapping ON answer_mapping.old_id = source.answer_id::text
JOIN map_question_options option_mapping ON option_mapping.old_id = source.option_id::text;

INSERT INTO matrix_answer_values_bigint (answer_id, matrix_row_id, numeric_value)
SELECT answer_mapping.new_id, row_mapping.new_id, source.numeric_value
FROM matrix_answer_values source
JOIN map_answers answer_mapping ON answer_mapping.old_id = source.answer_id::text
JOIN map_matrix_rows row_mapping ON row_mapping.old_id = source.matrix_row_id::text;

INSERT INTO notifications_bigint (
    id, recipient_user_id, type, channel, title, message,
    target_type, target_id, created_at, read_at
)
SELECT
    source.id::bigint,
    recipient_mapping.new_id,
    source.type,
    source.channel,
    source.title,
    source.message,
    source.target_type,
    CASE source.target_type
        WHEN 'SURVEY' THEN (
            SELECT mapping.new_id FROM map_surveys mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'REGISTRATION_REQUEST' THEN (
            SELECT mapping.new_id FROM map_registration_requests mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'COLLABORATION_REQUEST' THEN (
            SELECT mapping.new_id FROM map_collaboration_requests mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'PUBLICATION_REQUEST' THEN (
            SELECT mapping.new_id FROM map_publication_requests mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'ELEMENT_REQUEST' THEN (
            SELECT mapping.new_id FROM map_element_requests mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'USER' THEN (
            SELECT mapping.new_id FROM map_users mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'QUESTION' THEN (
            SELECT mapping.new_id FROM map_questions mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'BLOCK' THEN (
            SELECT mapping.new_id FROM map_blocks mapping WHERE mapping.old_id = source.target_id::text
        )
        WHEN 'TEMPLATE' THEN (
            SELECT mapping.new_id FROM map_templates mapping WHERE mapping.old_id = source.target_id::text
        )
        ELSE NULL
    END,
    source.created_at,
    source.read_at
FROM notifications source
JOIN map_users recipient_mapping ON recipient_mapping.old_id = source.recipient_user_id::text;

DROP TABLE
    answer_options,
    matrix_answer_values,
    answers,
    participations,
    question_settings,
    question_options,
    matrix_rows,
    questions,
    blocks,
    content_items,
    survey_collaborators,
    collaboration_requests,
    publication_requests,
    element_requests,
    notifications,
    surveys,
    templates,
    registration_requests,
    users
CASCADE;

ALTER TABLE users_bigint RENAME TO users;
ALTER TABLE registration_requests_bigint RENAME TO registration_requests;
ALTER TABLE templates_bigint RENAME TO templates;
ALTER TABLE surveys_bigint RENAME TO surveys;
ALTER TABLE survey_collaborators_bigint RENAME TO survey_collaborators;
ALTER TABLE collaboration_requests_bigint RENAME TO collaboration_requests;
ALTER TABLE publication_requests_bigint RENAME TO publication_requests;
ALTER TABLE content_items_bigint RENAME TO content_items;
ALTER TABLE blocks_bigint RENAME TO blocks;
ALTER TABLE questions_bigint RENAME TO questions;
ALTER TABLE question_settings_bigint RENAME TO question_settings;
ALTER TABLE question_options_bigint RENAME TO question_options;
ALTER TABLE matrix_rows_bigint RENAME TO matrix_rows;
ALTER TABLE element_requests_bigint RENAME TO element_requests;
ALTER TABLE participations_bigint RENAME TO participations;
ALTER TABLE answers_bigint RENAME TO answers;
ALTER TABLE answer_options_bigint RENAME TO answer_options;
ALTER TABLE matrix_answer_values_bigint RENAME TO matrix_answer_values;
ALTER TABLE notifications_bigint RENAME TO notifications;

ALTER TABLE users RENAME CONSTRAINT users_bigint_pkey TO users_pkey;
ALTER TABLE users RENAME CONSTRAINT tmp_uk_users_email TO uk_users_email;
ALTER TABLE registration_requests RENAME CONSTRAINT registration_requests_bigint_pkey TO registration_requests_pkey;
ALTER TABLE templates RENAME CONSTRAINT templates_bigint_pkey TO templates_pkey;
ALTER TABLE surveys RENAME CONSTRAINT surveys_bigint_pkey TO surveys_pkey;
ALTER TABLE surveys RENAME CONSTRAINT tmp_uk_surveys_public_code TO uk_surveys_public_code;
ALTER TABLE survey_collaborators RENAME CONSTRAINT survey_collaborators_bigint_pkey TO survey_collaborators_pkey;
ALTER TABLE collaboration_requests RENAME CONSTRAINT collaboration_requests_bigint_pkey TO collaboration_requests_pkey;
ALTER TABLE publication_requests RENAME CONSTRAINT publication_requests_bigint_pkey TO publication_requests_pkey;
ALTER TABLE content_items RENAME CONSTRAINT content_items_bigint_pkey TO content_items_pkey;
ALTER TABLE blocks RENAME CONSTRAINT blocks_bigint_pkey TO blocks_pkey;
ALTER TABLE blocks RENAME CONSTRAINT tmp_uk_blocks_parent_item TO uk_blocks_parent_item;
ALTER TABLE questions RENAME CONSTRAINT questions_bigint_pkey TO questions_pkey;
ALTER TABLE questions RENAME CONSTRAINT tmp_uk_questions_parent_item TO uk_questions_parent_item;
ALTER TABLE question_settings RENAME CONSTRAINT question_settings_bigint_pkey TO question_settings_pkey;
ALTER TABLE question_options RENAME CONSTRAINT question_options_bigint_pkey TO question_options_pkey;
ALTER TABLE question_options RENAME CONSTRAINT tmp_uk_question_options_position TO uk_question_options_position;
ALTER TABLE matrix_rows RENAME CONSTRAINT matrix_rows_bigint_pkey TO matrix_rows_pkey;
ALTER TABLE matrix_rows RENAME CONSTRAINT tmp_uk_matrix_rows_position TO uk_matrix_rows_position;
ALTER TABLE element_requests RENAME CONSTRAINT element_requests_bigint_pkey TO element_requests_pkey;
ALTER TABLE participations RENAME CONSTRAINT participations_bigint_pkey TO participations_pkey;
ALTER TABLE answers RENAME CONSTRAINT answers_bigint_pkey TO answers_pkey;
ALTER TABLE answers RENAME CONSTRAINT tmp_uk_answers_participation_question TO uk_answers_participation_question;
ALTER TABLE answer_options RENAME CONSTRAINT answer_options_bigint_pkey TO answer_options_pkey;
ALTER TABLE matrix_answer_values RENAME CONSTRAINT matrix_answer_values_bigint_pkey TO matrix_answer_values_pkey;
ALTER TABLE notifications RENAME CONSTRAINT notifications_bigint_pkey TO notifications_pkey;

ALTER SEQUENCE users_bigint_id_seq RENAME TO users_id_seq;
ALTER SEQUENCE registration_requests_bigint_id_seq RENAME TO registration_requests_id_seq;
ALTER SEQUENCE templates_bigint_id_seq RENAME TO templates_id_seq;
ALTER SEQUENCE surveys_bigint_id_seq RENAME TO surveys_id_seq;
ALTER SEQUENCE collaboration_requests_bigint_id_seq RENAME TO collaboration_requests_id_seq;
ALTER SEQUENCE publication_requests_bigint_id_seq RENAME TO publication_requests_id_seq;
ALTER SEQUENCE content_items_bigint_id_seq RENAME TO content_items_id_seq;
ALTER SEQUENCE blocks_bigint_id_seq RENAME TO blocks_id_seq;
ALTER SEQUENCE questions_bigint_id_seq RENAME TO questions_id_seq;
ALTER SEQUENCE question_options_bigint_id_seq RENAME TO question_options_id_seq;
ALTER SEQUENCE matrix_rows_bigint_id_seq RENAME TO matrix_rows_id_seq;
ALTER SEQUENCE element_requests_bigint_id_seq RENAME TO element_requests_id_seq;
ALTER SEQUENCE participations_bigint_id_seq RENAME TO participations_id_seq;
ALTER SEQUENCE answers_bigint_id_seq RENAME TO answers_id_seq;
ALTER SEQUENCE notifications_bigint_id_seq RENAME TO notifications_id_seq;

ALTER TABLE templates
    ADD CONSTRAINT fk_templates_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE surveys
    ADD CONSTRAINT fk_surveys_owner
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE survey_collaborators
    ADD CONSTRAINT fk_survey_collaborators_survey
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_survey_collaborators_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE collaboration_requests
    ADD CONSTRAINT fk_collaboration_requests_survey
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_collaboration_requests_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE publication_requests
    ADD CONSTRAINT fk_publication_requests_survey
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_publication_requests_user
        FOREIGN KEY (requested_by_user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE content_items
    ADD CONSTRAINT fk_content_items_survey
        FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_content_items_template
        FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_content_items_parent_block
        FOREIGN KEY (parent_block_id) REFERENCES blocks(id) ON DELETE CASCADE;

ALTER TABLE blocks
    ADD CONSTRAINT fk_blocks_parent_item
        FOREIGN KEY (parent_item_id) REFERENCES content_items(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_blocks_source
        FOREIGN KEY (source_block_id) REFERENCES blocks(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_blocks_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE questions
    ADD CONSTRAINT fk_questions_parent_item
        FOREIGN KEY (parent_item_id) REFERENCES content_items(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_questions_source_fixed
        FOREIGN KEY (source_fixed_question_id) REFERENCES questions(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_questions_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE question_settings
    ADD CONSTRAINT fk_question_settings_question
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

ALTER TABLE question_options
    ADD CONSTRAINT fk_question_options_question
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

ALTER TABLE matrix_rows
    ADD CONSTRAINT fk_matrix_rows_question
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE;

ALTER TABLE element_requests
    ADD CONSTRAINT fk_element_requests_user
    FOREIGN KEY (requested_by_user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE participations
    ADD CONSTRAINT fk_participations_survey
    FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE;

ALTER TABLE answers
    ADD CONSTRAINT fk_answers_participation
        FOREIGN KEY (participation_id) REFERENCES participations(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES questions(id);

ALTER TABLE answer_options
    ADD CONSTRAINT fk_answer_options_answer
        FOREIGN KEY (answer_id) REFERENCES answers(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_answer_options_option
        FOREIGN KEY (option_id) REFERENCES question_options(id);

ALTER TABLE matrix_answer_values
    ADD CONSTRAINT fk_matrix_answer_values_answer
        FOREIGN KEY (answer_id) REFERENCES answers(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_matrix_answer_values_row
        FOREIGN KEY (matrix_row_id) REFERENCES matrix_rows(id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_recipient
    FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX ux_users_single_superadmin
    ON users(role)
    WHERE role = 'SUPERADMIN';

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_name ON users(name);

CREATE INDEX idx_registration_requests_email ON registration_requests(email);
CREATE INDEX idx_registration_requests_created_at ON registration_requests(created_at);
CREATE UNIQUE INDEX ux_registration_requests_email ON registration_requests(email);

CREATE INDEX idx_templates_created_by ON templates(created_by_user_id);
CREATE INDEX idx_templates_title ON templates(title);

CREATE INDEX idx_surveys_owner ON surveys(owner_id);
CREATE INDEX idx_surveys_lifecycle_visibility ON surveys(lifecycle_status, visibility);
CREATE INDEX idx_surveys_updated_at ON surveys(updated_at);
CREATE INDEX idx_surveys_tags ON surveys USING gin(tags);

CREATE INDEX idx_survey_collaborators_user ON survey_collaborators(user_id);

CREATE INDEX idx_collaboration_requests_survey ON collaboration_requests(survey_id);
CREATE INDEX idx_collaboration_requests_user ON collaboration_requests(user_id);
CREATE UNIQUE INDEX ux_collaboration_requests_survey_user
    ON collaboration_requests(survey_id, user_id);

CREATE INDEX idx_publication_requests_user ON publication_requests(requested_by_user_id);
CREATE UNIQUE INDEX ux_publication_requests_survey ON publication_requests(survey_id);

CREATE UNIQUE INDEX ux_content_items_survey_position
    ON content_items(survey_id, position)
    WHERE survey_id IS NOT NULL;
CREATE UNIQUE INDEX ux_content_items_template_position
    ON content_items(template_id, position)
    WHERE template_id IS NOT NULL;
CREATE UNIQUE INDEX ux_content_items_block_position
    ON content_items(parent_block_id, position)
    WHERE parent_block_id IS NOT NULL;

CREATE INDEX idx_blocks_scope ON blocks(scope);
CREATE INDEX idx_blocks_source ON blocks(source_block_id);
CREATE INDEX idx_blocks_created_by ON blocks(created_by_user_id);

CREATE INDEX idx_questions_scope ON questions(scope);
CREATE INDEX idx_questions_source_fixed ON questions(source_fixed_question_id);
CREATE INDEX idx_questions_created_by ON questions(created_by_user_id);

CREATE UNIQUE INDEX ux_question_options_one_other
    ON question_options(question_id)
    WHERE is_other = true;
CREATE INDEX idx_question_options_question ON question_options(question_id, position);

CREATE INDEX idx_matrix_rows_question ON matrix_rows(question_id, position);

CREATE INDEX idx_element_requests_created_at ON element_requests(created_at);
CREATE INDEX idx_element_requests_user ON element_requests(requested_by_user_id);

CREATE INDEX idx_participations_survey_submitted
    ON participations(survey_id, submitted_at);

CREATE INDEX idx_answers_participation ON answers(participation_id);
CREATE INDEX idx_answers_question ON answers(question_id);

CREATE INDEX idx_notifications_recipient_created
    ON notifications(recipient_user_id, created_at);
CREATE INDEX idx_notifications_recipient_channel_read
    ON notifications(recipient_user_id, channel, read_at);
CREATE INDEX idx_notifications_target
    ON notifications(target_type, target_id);
CREATE INDEX idx_notifications_unread
    ON notifications(recipient_user_id, channel)
    WHERE read_at IS NULL;

CREATE OR REPLACE FUNCTION prevent_no_superadmin()
RETURNS trigger AS $$
BEGIN
    IF (SELECT count(*) FROM users WHERE role = 'SUPERADMIN') = 0 THEN
        RAISE EXCEPTION 'Debe existir siempre un superadministrador';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_keep_superadmin_after_delete
AFTER DELETE ON users
FOR EACH STATEMENT EXECUTE FUNCTION prevent_no_superadmin();

CREATE TRIGGER trg_users_keep_superadmin_after_update
AFTER UPDATE OF role ON users
FOR EACH STATEMENT EXECUTE FUNCTION prevent_no_superadmin();

SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM users;
SELECT setval(pg_get_serial_sequence('registration_requests', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM registration_requests;
SELECT setval(pg_get_serial_sequence('templates', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM templates;
SELECT setval(pg_get_serial_sequence('surveys', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM surveys;
SELECT setval(pg_get_serial_sequence('collaboration_requests', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM collaboration_requests;
SELECT setval(pg_get_serial_sequence('publication_requests', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM publication_requests;
SELECT setval(pg_get_serial_sequence('content_items', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM content_items;
SELECT setval(pg_get_serial_sequence('blocks', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM blocks;
SELECT setval(pg_get_serial_sequence('questions', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM questions;
SELECT setval(pg_get_serial_sequence('question_options', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM question_options;
SELECT setval(pg_get_serial_sequence('matrix_rows', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM matrix_rows;
SELECT setval(pg_get_serial_sequence('element_requests', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM element_requests;
SELECT setval(pg_get_serial_sequence('participations', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM participations;
SELECT setval(pg_get_serial_sequence('answers', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM answers;
SELECT setval(pg_get_serial_sequence('notifications', 'id'), COALESCE(max(id), 1), count(*) > 0) FROM notifications;

DROP SEQUENCE IF EXISTS survey_id_sequence;
DROP SEQUENCE IF EXISTS block_id_sequence;
DROP SEQUENCE IF EXISTS question_id_sequence;