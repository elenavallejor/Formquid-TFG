-- Sustituye la tabla auxiliar por protección transaccional en users.
-- Flyway ejecuta la sustitución completa en una única transacción.
LOCK TABLE users IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users)
       AND NOT EXISTS (SELECT 1 FROM users WHERE role = 'SUPERADMIN') THEN
        RAISE EXCEPTION 'La base contiene usuarios pero no un superadministrador';
    END IF;
END;
$$;

DROP TRIGGER trg_users_register_first_superadmin ON users;
DROP TRIGGER trg_users_reassign_superadmin_guard ON users;
DROP TABLE superadmin_guard;
DROP FUNCTION register_first_superadmin();
DROP FUNCTION reassign_superadmin_guard();
DROP FUNCTION prevent_superadmin_guard_removal();
ALTER TABLE users DROP CONSTRAINT uk_users_role_id_guard;

CREATE FUNCTION serialize_user_role_changes()
RETURNS trigger AS $$
BEGIN
    -- Clave reservada para las operaciones de usuarios de FormQuid.
    -- Se adquiere antes de modificar filas y se libera al terminar la transacción.
    PERFORM pg_advisory_xact_lock(17493, 1);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE FUNCTION require_superadmin()
RETURNS trigger AS $$
BEGIN
    IF TG_OP <> 'TRUNCATE' THEN
        -- Una sola búsqueda, usando idx_users_role, después de toda la sentencia.
        -- FOR SHARE impide validar una versión obsoleta en REPEATABLE READ y
        -- SERIALIZABLE: si el testigo cambió, PostgreSQL aborta la transacción.
        PERFORM id FROM users WHERE role = 'SUPERADMIN' LIMIT 1 FOR SHARE;
        IF FOUND THEN
            RETURN NULL;
        END IF;
    END IF;
    RAISE EXCEPTION 'Debe existir siempre un superadministrador'
        USING ERRCODE = '23514', CONSTRAINT = 'ck_users_require_superadmin';
END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE TRIGGER trg_users_serialize_role_changes
BEFORE INSERT OR DELETE OR UPDATE OF role ON users
FOR EACH STATEMENT EXECUTE FUNCTION serialize_user_role_changes();

CREATE TRIGGER trg_users_require_superadmin
AFTER INSERT OR DELETE OR UPDATE OF role ON users
FOR EACH STATEMENT EXECUTE FUNCTION require_superadmin();

CREATE TRIGGER trg_users_prevent_truncate
BEFORE TRUNCATE ON users
FOR EACH STATEMENT EXECUTE FUNCTION require_superadmin();
