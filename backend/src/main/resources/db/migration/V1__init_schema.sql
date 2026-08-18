create sequence if not exists survey_id_sequence;
create sequence if not exists block_id_sequence;
create sequence if not exists question_id_sequence;

create table users (
    id uuid primary key,
    role varchar(30) not null check (role in ('EDITOR', 'ADMIN', 'SUPERADMIN')),
    email varchar(320) not null unique check (email = lower(btrim(email))),
    name varchar(150) not null,
    gender varchar(20) not null check (gender in ('MALE', 'FEMALE', 'OTHER')),
    institution varchar(200) not null,
    password_hash varchar(255) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index ux_users_single_superadmin on users (role) where role = 'SUPERADMIN';
create index idx_users_role on users(role);
create index idx_users_email on users(email);

create or replace function prevent_no_superadmin()
returns trigger as $$
begin
    if (select count(*) from users where role = 'SUPERADMIN') = 0 then
        raise exception 'Debe existir siempre un superadministrador';
    end if;
    return null;
end;
$$ language plpgsql;

create trigger trg_users_keep_superadmin_after_delete
after delete on users
for each statement execute function prevent_no_superadmin();

create trigger trg_users_keep_superadmin_after_update
after update of role on users
for each statement execute function prevent_no_superadmin();

create table registration_requests (
    id uuid primary key,
    email varchar(320) not null check (email = lower(btrim(email))),
    name varchar(150) not null,
    gender varchar(20) not null check (gender in ('MALE', 'FEMALE', 'OTHER')),
    institution varchar(200) not null,
    password_hash varchar(255) not null,
    status varchar(30) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    created_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_registration_requests_email on registration_requests(email);
create unique index ux_registration_requests_pending_email on registration_requests(email) where status = 'PENDING';

create table templates (
    id uuid primary key,
    title varchar(200) not null,
    description text,
    image_key text,
    created_by_user_id uuid references users(id) on delete set null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_templates_created_by on templates(created_by_user_id);
create index idx_templates_title on templates(title);

create table surveys (
    id varchar(20) primary key check (id ~ '^S[1-9][0-9]*$'),
    owner_id uuid not null references users(id) on delete restrict,
    source_template_id uuid references templates(id) on delete set null,
    title varchar(200) not null,
    description text,
    image_key text,
    lifecycle_status varchar(30) not null check (lifecycle_status in ('DRAFT', 'OPEN', 'CLOSED')),
    access_mode varchar(40) not null check (access_mode in ('PUBLIC', 'PASSWORD_PROTECTED', 'AUTHENTICATED')),
    visibility varchar(30) not null check (visibility in ('LISTED', 'UNLISTED')),
    access_password_hash varchar(255),
    tags varchar(50)[] not null default '{}' check (cardinality(tags) <= 4),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (
        (access_mode = 'PASSWORD_PROTECTED' and access_password_hash is not null)
        or (access_mode <> 'PASSWORD_PROTECTED' and access_password_hash is null)
    )
);

create index idx_surveys_owner on surveys(owner_id);
create index idx_surveys_lifecycle_visibility on surveys(lifecycle_status, visibility);
create index idx_surveys_tags on surveys using gin(tags);

create table survey_collaborators (
    survey_id varchar(20) not null references surveys(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    joined_at timestamptz not null,
    primary key (survey_id, user_id)
);

create index idx_survey_collaborators_user on survey_collaborators(user_id);

create table collaboration_requests (
    id uuid primary key,
    survey_id varchar(20) not null references surveys(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    request_type varchar(30) not null check (request_type in ('INVITATION', 'ACCESS_REQUEST')),
    message text,
    status varchar(30) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    created_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_collaboration_requests_survey on collaboration_requests(survey_id);
create index idx_collaboration_requests_user on collaboration_requests(user_id);
create unique index ux_collaboration_requests_pending_survey_user on collaboration_requests(survey_id, user_id) where status = 'PENDING';

create table publication_requests (
    id uuid primary key,
    survey_id varchar(20) not null references surveys(id) on delete cascade,
    requested_by_user_id uuid not null references users(id) on delete cascade,
    message text,
    status varchar(30) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    created_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_publication_requests_user on publication_requests(requested_by_user_id);
create unique index ux_publication_requests_pending_survey on publication_requests(survey_id) where status = 'PENDING';

create table content_items (
    id uuid primary key,
    survey_id varchar(20) references surveys(id) on delete cascade,
    template_id uuid references templates(id) on delete cascade,
    parent_block_id varchar(20),
    item_type varchar(30) not null check (item_type in ('QUESTION', 'BLOCK', 'TEXT')),
    position integer not null check (position >= 1),
    is_required boolean,
    text_body text,
    check (((survey_id is not null)::int + (template_id is not null)::int + (parent_block_id is not null)::int) = 1),
    check (
        (item_type = 'QUESTION' and is_required is not null and text_body is null)
        or (item_type = 'BLOCK' and is_required is null and text_body is null)
        or (item_type = 'TEXT' and is_required is null and text_body is not null)
    )
);

create unique index ux_content_items_survey_position on content_items(survey_id, position) where survey_id is not null;
create unique index ux_content_items_template_position on content_items(template_id, position) where template_id is not null;
create unique index ux_content_items_block_position on content_items(parent_block_id, position) where parent_block_id is not null;

create table blocks (
    id varchar(20) primary key check (id ~ '^B[1-9][0-9]*$'),
    parent_item_id uuid unique references content_items(id) on delete cascade,
    scope varchar(30) not null check (scope in ('CATALOG', 'SNAPSHOT')),
    source_block_id varchar(20) references blocks(id) on delete set null,
    name varchar(150) not null,
    description text,
    created_by_user_id uuid references users(id) on delete set null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (
        (scope = 'CATALOG' and parent_item_id is null)
        or (scope = 'SNAPSHOT' and parent_item_id is not null)
    )
);

alter table content_items
    add constraint fk_content_items_parent_block
    foreign key (parent_block_id) references blocks(id) on delete cascade;

create index idx_blocks_scope on blocks(scope);
create index idx_blocks_source on blocks(source_block_id);
create index idx_blocks_created_by on blocks(created_by_user_id);

create table questions (
    id varchar(20) primary key check (id ~ '^Q[1-9][0-9]*$'),
    parent_item_id uuid unique references content_items(id) on delete cascade,
    scope varchar(30) not null check (scope in ('CATALOG_FIXED', 'LOCAL', 'FIXED_SNAPSHOT')),
    catalog_name varchar(150),
    statement text not null,
    annotation text,
    question_type varchar(40) not null check (question_type in ('SHORT_TEXT', 'LONG_TEXT', 'NUMBER', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SCALE', 'MATRIX_SCALE', 'BIPOLAR_SCALE', 'BOOLEAN')),
    evaluation_direction varchar(30) not null check (evaluation_direction in ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    evaluation_weight numeric,
    source_fixed_question_id varchar(20) references questions(id) on delete set null,
    created_by_user_id uuid references users(id) on delete set null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    check (
        (scope = 'CATALOG_FIXED' and parent_item_id is null and catalog_name is not null and source_fixed_question_id is null)
        or (scope = 'LOCAL' and parent_item_id is not null and catalog_name is null)
        or (scope = 'FIXED_SNAPSHOT' and parent_item_id is not null and catalog_name is null)
    ),
    check (
        (evaluation_direction = 'NEUTRAL' and evaluation_weight is null)
        or (evaluation_direction in ('POSITIVE', 'NEGATIVE') and evaluation_weight > 0)
    )
);

create index idx_questions_scope on questions(scope);
create index idx_questions_source_fixed on questions(source_fixed_question_id);
create index idx_questions_created_by on questions(created_by_user_id);

create table question_settings (
    question_id varchar(20) primary key references questions(id) on delete cascade,
    max_length integer check (max_length is null or max_length > 0),
    min_value numeric,
    max_value numeric,
    step numeric check (step is null or step > 0),
    min_selections integer check (min_selections is null or min_selections > 0),
    max_selections integer check (max_selections is null or max_selections > 0),
    left_label text,
    right_label text,
    true_label varchar(100),
    false_label varchar(100),
    true_evaluation_factor numeric check (true_evaluation_factor is null or true_evaluation_factor between 0 and 1),
    false_evaluation_factor numeric check (false_evaluation_factor is null or false_evaluation_factor between 0 and 1),
    font_family varchar(80),
    font_size smallint,
	text_color varchar(7) check (text_color is null or text_color ~ '^#[0-9A-Fa-f]{6}$'),
    button_shape varchar(30) check (button_shape is null or button_shape in ('RECTANGULAR', 'ROUNDED', 'PILL')),
    scale_display_type varchar(30) check (scale_display_type is null or scale_display_type in ('NUMERIC', 'STARS', 'FACES')),
    check (min_value is null or max_value is null or min_value <= max_value),
    check (min_selections is null or max_selections is null or min_selections <= max_selections)
);

create table question_options (
    id uuid primary key,
    question_id varchar(20) not null references questions(id) on delete cascade,
    label text not null,
    position integer not null check (position >= 1),
    is_other boolean not null,
    evaluation_factor numeric check (evaluation_factor is null or evaluation_factor between 0 and 1),
    unique (question_id, position)
);

create unique index ux_question_options_one_other on question_options(question_id) where is_other = true;
create index idx_question_options_question_position on question_options(question_id, position);

create table matrix_rows (
    id uuid primary key,
    question_id varchar(20) not null references questions(id) on delete cascade,
    label text not null,
    position integer not null check (position >= 1),
    unique (question_id, position)
);

create index idx_matrix_rows_question_position on matrix_rows(question_id, position);

create table element_requests (
    id uuid primary key,
    requested_by_user_id uuid not null references users(id) on delete cascade,
    element_type varchar(40) not null check (element_type in ('FIXED_QUESTION', 'BLOCK', 'TEMPLATE')),
    message text not null,
    request_payload jsonb not null,
    status varchar(30) not null check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    created_at timestamptz not null,
    resolved_at timestamptz
);

create index idx_element_requests_created_at on element_requests(created_at);
create index idx_element_requests_user on element_requests(requested_by_user_id);

create table participations (
    id uuid primary key,
    survey_id varchar(20) not null references surveys(id) on delete cascade,
    submitted_at timestamptz not null
);

create index idx_participations_survey_submitted on participations(survey_id, submitted_at);

create table answers (
    id uuid primary key,
    participation_id uuid not null references participations(id) on delete cascade,
    question_id varchar(20) not null references questions(id),
    text_value text,
    numeric_value numeric,
    boolean_value boolean,
    unique (participation_id, question_id),
    check (((text_value is not null)::int + (numeric_value is not null)::int + (boolean_value is not null)::int) <= 1)
);

create index idx_answers_participation on answers(participation_id);
create index idx_answers_question on answers(question_id);

create table answer_options (
    answer_id uuid not null references answers(id) on delete cascade,
    option_id uuid not null references question_options(id),
    other_text text,
    primary key (answer_id, option_id)
);

create table matrix_answer_values (
    answer_id uuid not null references answers(id) on delete cascade,
    matrix_row_id uuid not null references matrix_rows(id),
    numeric_value numeric not null,
    primary key (answer_id, matrix_row_id)
);

create table notifications (
    id bigserial primary key,
    recipient_user_id uuid not null references users(id) on delete cascade,
    type varchar(50) not null check (type in (
        'GENERAL', 'REGISTRATION_REQUEST', 'REGISTRATION_APPROVED', 'REGISTRATION_REJECTED',
        'ACCOUNT_UPDATED', 'COLLABORATION_INVITATION', 'COLLABORATION_REQUEST',
        'COLLABORATION_ACCEPTED', 'COLLABORATION_REJECTED', 'PUBLICATION_REQUEST',
        'PUBLICATION_ACCEPTED', 'PUBLICATION_REJECTED', 'ELEMENT_REQUEST',
        'ELEMENT_REQUEST_RESOLVED', 'SURVEY_STATUS_CHANGED', 'SURVEY_OWNERSHIP_TRANSFERRED'
    )),
    channel varchar(30) not null check (channel in ('INTERNAL', 'EMAIL')),
    title varchar(180) not null,
    message varchar(1500) not null,
    target_type varchar(50) not null check (target_type in (
        'SURVEY', 'REGISTRATION_REQUEST', 'COLLABORATION_REQUEST', 'PUBLICATION_REQUEST',
        'ELEMENT_REQUEST', 'USER', 'QUESTION', 'BLOCK', 'TEMPLATE', 'NONE'
    )),
    target_id varchar(80),
    created_at timestamptz not null,
    read_at timestamptz
);

create index idx_notifications_recipient_created on notifications(recipient_user_id, created_at);
create index idx_notifications_recipient_read on notifications(recipient_user_id, read_at);
create index idx_notifications_target on notifications(target_type, target_id);
