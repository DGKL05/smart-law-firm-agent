USE smart_law_admin;

CREATE TABLE IF NOT EXISTS admin_audit_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  admin_user_id BIGINT,
  action VARCHAR(128) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(64),
  detail TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
