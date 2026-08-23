delete from registration_requests
where status <> 'PENDING';

delete from collaboration_requests
where status <> 'PENDING';

delete from publication_requests
where status <> 'PENDING';

delete from element_requests
where status <> 'PENDING';


drop index if exists idx_registration_requests_email;
drop index if exists ux_registration_requests_pending_email;

alter table registration_requests
    add column motivation varchar(1000);

update registration_requests
set motivation = ''
where motivation is null;

alter table registration_requests
    alter column motivation set not null,
    drop column status,
    drop column resolved_at,
    add constraint uq_registration_requests_email unique (email);


drop index if exists idx_collaboration_requests_survey;
drop index if exists idx_collaboration_requests_user;
drop index if exists ux_collaboration_requests_pending_survey_user;

alter table collaboration_requests
    drop column status,
    drop column resolved_at,
    add constraint uq_collaboration_requests_survey_user unique (survey_id, user_id);


drop index if exists idx_publication_requests_user;
drop index if exists ux_publication_requests_pending_survey;

alter table publication_requests
    drop column status,
    drop column resolved_at,
    add constraint uq_publication_requests_survey unique (survey_id);


drop index if exists idx_element_requests_created_at;
drop index if exists idx_element_requests_user;

alter table element_requests
    drop column status,
    drop column resolved_at;


drop index if exists idx_users_email;

drop index if exists idx_question_options_question_position;

drop index if exists idx_matrix_rows_question_position;

drop index if exists idx_answers_participation;


drop index if exists idx_notifications_recipient_read;

create index idx_notifications_unread
    on notifications(recipient_user_id, channel)
    where read_at is null;