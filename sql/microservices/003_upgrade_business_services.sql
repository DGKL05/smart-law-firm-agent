SET NAMES utf8mb4;
USE smart_law_appointment;

DROP PROCEDURE IF EXISTS upgrade_business_services;
DELIMITER $$
CREATE PROCEDURE upgrade_business_services()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'appointment_lawyer_guard' AND column_name = 'guard_version'
  ) THEN
    ALTER TABLE smart_law_appointment.appointment_lawyer_guard ADD COLUMN guard_version BIGINT NOT NULL DEFAULT 0;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_schedule' AND table_name = 'schedule_lawyer_guard' AND column_name = 'guard_version'
  ) THEN
    ALTER TABLE smart_law_schedule.schedule_lawyer_guard ADD COLUMN guard_version BIGINT NOT NULL DEFAULT 0;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_admin' AND table_name = 'admin_audit_event' AND column_name = 'source'
  ) THEN
    ALTER TABLE smart_law_admin.admin_audit_event ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'GATEWAY',
      ADD INDEX idx_admin_create (admin_user_id, create_time);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_lawyer' AND table_name = 'law_firm' AND column_name = 'photo_url'
  ) THEN
    ALTER TABLE smart_law_lawyer.law_firm ADD COLUMN photo_url VARCHAR(255);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_lawyer' AND table_name = 'lawyer' AND column_name = 'active_user_id'
  ) THEN
    ALTER TABLE smart_law_lawyer.lawyer
      ADD COLUMN active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id END) STORED,
      ADD UNIQUE INDEX uk_lawyer_active_user_id (active_user_id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'appointment' AND column_name = 'next_sync_time'
  ) THEN
    ALTER TABLE appointment ADD COLUMN next_sync_time DATETIME, ADD INDEX idx_pending_sync (status, next_sync_time);
  END IF;
  IF EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'appointment' AND index_name = 'uk_lawyer_time'
  ) THEN
    ALTER TABLE appointment DROP INDEX uk_lawyer_time, ADD INDEX idx_lawyer_slot (lawyer_id, appointment_time, status);
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'appointment' AND column_name = 'failure_reason'
  ) THEN
    ALTER TABLE appointment ADD COLUMN failure_reason VARCHAR(256);
  END IF;

  UPDATE appointment SET status = '预约失败', failure_reason = '旧版预约未确认，请重新预约', next_sync_time = NULL
  WHERE status = 'PENDING' AND deleted = 0;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'legal_case' AND column_name = 'case_no'
  ) THEN
    ALTER TABLE legal_case ADD COLUMN case_no VARCHAR(64), ADD COLUMN case_type VARCHAR(64),
      ADD COLUMN description TEXT, ADD INDEX idx_user_case (user_id, create_time);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'consultation' AND column_name = 'title'
  ) THEN
    ALTER TABLE consultation ADD COLUMN title VARCHAR(128) NOT NULL DEFAULT '法律咨询',
      ADD COLUMN law_firm_id BIGINT UNSIGNED, ADD INDEX idx_user_consultation (user_id, create_time),
      ADD INDEX idx_lawyer_consultation (lawyer_id, status, create_time);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'lawyer_schedule'
  ) THEN
    INSERT IGNORE INTO smart_law_schedule.lawyer_schedule
      (id, lawyer_id, weekday, start_time, end_time, enabled, create_time, update_time, deleted)
    SELECT id, lawyer_id, weekday, start_time, end_time, enabled, create_time, update_time, deleted
    FROM smart_law_appointment.lawyer_schedule;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'smart_law_appointment' AND table_name = 'notification'
  ) THEN
    INSERT IGNORE INTO smart_law_notification.notification
      (event_key, user_id, title, content, read_flag, create_time, update_time, deleted)
    SELECT CONCAT('legacy-notification:', id), user_id, title, COALESCE(content, ''),
      COALESCE(read_flag, 0), create_time, update_time, COALESCE(deleted, 0)
    FROM smart_law_appointment.notification;
  END IF;

  INSERT IGNORE INTO smart_law_schedule.schedule_reservation (appointment_id, lawyer_id, slot_start, status)
  SELECT id, lawyer_id, appointment_time, 'RESERVED'
  FROM smart_law_appointment.appointment
  WHERE status = '已预约' AND deleted = 0;
END$$
DELIMITER ;

CALL upgrade_business_services();
DROP PROCEDURE upgrade_business_services;
