-- Elimina el género de los usuarios y de las solicitudes de registro.

ALTER TABLE users
    DROP CONSTRAINT ck_users_values,
    DROP COLUMN gender,
    ADD CONSTRAINT ck_users_values CHECK (
        role IN ('EDITOR', 'ADMIN', 'SUPERADMIN') AND email = lower(btrim(email))
    );

ALTER TABLE registration_requests
    DROP CONSTRAINT ck_registration_requests_values,
    DROP COLUMN gender,
    ADD CONSTRAINT ck_registration_requests_values CHECK (email = lower(btrim(email)));
