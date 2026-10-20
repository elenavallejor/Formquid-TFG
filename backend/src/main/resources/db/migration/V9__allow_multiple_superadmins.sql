-- Permite varios superadministradores y evita que se elimine el último.

DROP INDEX IF EXISTS ux_users_single_superadmin;

ALTER TABLE users
    ADD CONSTRAINT uk_users_role_id UNIQUE (role, id);

CREATE TABLE superadmin_guard (
    singleton boolean PRIMARY KEY DEFAULT true CHECK (singleton),
    role varchar(30) NOT NULL DEFAULT 'SUPERADMIN' CHECK (role = 'SUPERADMIN'),
    user_id bigint NOT NULL UNIQUE,
    CONSTRAINT fk_superadmin_guard_user
        FOREIGN KEY (role, user_id)
        REFERENCES users(role, id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

INSERT INTO superadmin_guard (user_id)
SELECT id
FROM users
WHERE role = 'SUPERADMIN'
ORDER BY id
LIMIT 1;

DO $$
BEGIN
    -- Una base nueva todavía no tiene usuarios: el inicializador creará el primero
    -- cuando Flyway haya terminado. Si ya hay usuarios, exigir un superadministrador.
    IF EXISTS (SELECT 1 FROM users)
       AND NOT EXISTS (SELECT 1 FROM superadmin_guard) THEN
        RAISE EXCEPTION 'Debe existir al menos un superadministrador antes de aplicar la migracion';
    END IF;
END;
$$;

DROP TRIGGER IF EXISTS trg_users_keep_superadmin_after_delete ON users;
DROP TRIGGER IF EXISTS trg_users_keep_superadmin_after_update ON users;
DROP FUNCTION IF EXISTS prevent_no_superadmin();

CREATE FUNCTION reassign_superadmin_guard()
RETURNS trigger AS $$
DECLARE
    guarded_user_id bigint;
    replacement_user_id bigint;
BEGIN
    IF OLD.role <> 'SUPERADMIN' THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' AND NEW.role = 'SUPERADMIN' THEN
        RETURN NEW;
    END IF;

    SELECT user_id INTO guarded_user_id
    FROM superadmin_guard
    WHERE singleton
    FOR UPDATE;

    IF guarded_user_id = OLD.id THEN
        SELECT id INTO replacement_user_id
        FROM users
        WHERE role = 'SUPERADMIN'
          AND id <> OLD.id
        ORDER BY id
        LIMIT 1
        FOR UPDATE;

        IF replacement_user_id IS NULL THEN
            RAISE EXCEPTION 'Debe existir siempre un superadministrador';
        END IF;

        UPDATE superadmin_guard
        SET user_id = replacement_user_id
        WHERE singleton;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_reassign_superadmin_guard
BEFORE DELETE OR UPDATE OF role ON users
FOR EACH ROW EXECUTE FUNCTION reassign_superadmin_guard();

CREATE FUNCTION prevent_superadmin_guard_removal()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'No se puede eliminar la garantia del superadministrador';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_superadmin_guard_prevent_delete
BEFORE DELETE ON superadmin_guard
FOR EACH ROW EXECUTE FUNCTION prevent_superadmin_guard_removal();

CREATE TRIGGER trg_superadmin_guard_prevent_truncate
BEFORE TRUNCATE ON superadmin_guard
FOR EACH STATEMENT EXECUTE FUNCTION prevent_superadmin_guard_removal();
