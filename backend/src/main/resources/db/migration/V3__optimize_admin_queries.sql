-- Agiliza los listados de administración y la consulta de notificaciones sin leer.

CREATE INDEX IF NOT EXISTS idx_registration_requests_created_at
    ON registration_requests (created_at);

CREATE INDEX IF NOT EXISTS idx_surveys_updated_at
    ON surveys (updated_at);

CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications (recipient_user_id, channel)
    WHERE read_at IS NULL;

DROP INDEX IF EXISTS idx_users_email;
