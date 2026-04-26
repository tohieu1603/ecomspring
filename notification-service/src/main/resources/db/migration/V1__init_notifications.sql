CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  type VARCHAR(16) NOT NULL,
  channel VARCHAR(255),
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  error_message TEXT,
  reference_type VARCHAR(32),
  reference_id VARCHAR(64),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  sent_at TIMESTAMPTZ,
  read_at TIMESTAMPTZ
);

CREATE INDEX ix_notif_user ON notifications(user_id);
CREATE INDEX ix_notif_user_read ON notifications(user_id, is_read);
CREATE INDEX ix_notif_user_type ON notifications(user_id, type);
CREATE INDEX ix_notif_created ON notifications(created_at DESC, id DESC);
