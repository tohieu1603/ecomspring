CREATE UNIQUE INDEX IF NOT EXISTS uniq_notif_event
    ON notifications (user_id, reference_type, reference_id, type);
