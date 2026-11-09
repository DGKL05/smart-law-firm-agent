USE smart_law_lawyer;

CREATE TABLE IF NOT EXISTS law_firm (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  province_code VARCHAR(16),
  province_name VARCHAR(32),
  city VARCHAR(64),
  address VARCHAR(255),
  phone VARCHAR(32),
  email VARCHAR(128),
  logo_url VARCHAR(255),
  photo_url VARCHAR(255),
  specialties VARCHAR(255),
  description TEXT,
  license_no VARCHAR(128),
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lawyer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  law_firm_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  gender VARCHAR(16),
  avatar_url VARCHAR(255),
  phone VARCHAR(32),
  email VARCHAR(128),
  category VARCHAR(64),
  title VARCHAR(64),
  experience_years INT DEFAULT 0,
  description TEXT,
  good_at VARCHAR(255),
  available_time_slots VARCHAR(255),
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN user_id ELSE NULL END) STORED,
  KEY idx_law_firm_id (law_firm_id),
  KEY idx_user_id (user_id),
  UNIQUE KEY uk_lawyer_active_user_id (active_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lawyer_media (
  id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  content_type VARCHAR(16) NOT NULL,
  image_data MEDIUMBLOB NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
