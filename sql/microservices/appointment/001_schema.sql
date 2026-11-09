USE smart_law_appointment;

CREATE TABLE IF NOT EXISTS appointment (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  lawyer_id BIGINT UNSIGNED NOT NULL,
  law_firm_id BIGINT UNSIGNED NOT NULL,
  law_firm_name VARCHAR(128),
  lawyer_name VARCHAR(64),
  appointment_time DATETIME NOT NULL,
  duration_minutes INT UNSIGNED NOT NULL DEFAULT 60,
  status VARCHAR(32) NOT NULL,
  remark VARCHAR(500),
  failure_reason VARCHAR(256),
  next_sync_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  KEY idx_lawyer_slot (lawyer_id, appointment_time, status),
  KEY idx_user_create (user_id, create_time),
  KEY idx_pending_sync (status, next_sync_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointment_lawyer_guard (
  lawyer_id BIGINT UNSIGNED PRIMARY KEY,
  guard_version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS undo_log (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  context VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB NOT NULL,
  log_status INT NOT NULL,
  log_created DATETIME(6) NOT NULL,
  log_modified DATETIME(6) NOT NULL,
  UNIQUE KEY ux_undo_log (xid, branch_id),
  KEY ix_log_created (log_created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS appointment_notification_outbox (
  event_key VARCHAR(128) PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(500) NOT NULL,
  next_attempt_time DATETIME NOT NULL,
  sent_time DATETIME,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pending_notification (sent_time, next_attempt_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS legal_case (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED,
  lawyer_id BIGINT UNSIGNED,
  case_no VARCHAR(64),
  title VARCHAR(128) NOT NULL,
  case_type VARCHAR(64),
  description TEXT,
  start_time DATETIME,
  end_time DATETIME,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  KEY idx_lawyer_case_time (lawyer_id, start_time, end_time),
  KEY idx_user_case (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  law_firm_id BIGINT UNSIGNED,
  lawyer_id BIGINT UNSIGNED,
  title VARCHAR(128) NOT NULL,
  question TEXT NOT NULL,
  reply TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
  KEY idx_user_consultation (user_id, create_time),
  KEY idx_lawyer_consultation (lawyer_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
