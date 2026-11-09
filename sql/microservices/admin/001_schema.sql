USE smart_law_admin;

CREATE TABLE IF NOT EXISTS admin_audit_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT,
  action VARCHAR(128) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(64),
  detail TEXT,
  source VARCHAR(16) NOT NULL DEFAULT 'GATEWAY',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_admin_create (admin_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
