-- Agiliza el filtrado de notificaciones por usuario, tipo y estado de lectura.

CREATE INDEX idx_notifications_recipient_unread_created
    ON notifications (recipient_user_id, created_at DESC, id DESC)
    WHERE read_at IS NULL;

CREATE INDEX idx_notifications_recipient_type_read_created
    ON notifications (recipient_user_id, type, (read_at IS NOT NULL), created_at DESC, id DESC);
