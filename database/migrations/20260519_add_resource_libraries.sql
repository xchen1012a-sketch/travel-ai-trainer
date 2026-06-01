USE teach_agent;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  route_name VARCHAR(128) NULL,
  old_info TEXT NULL,
  new_info TEXT NULL,
  product_context TEXT NULL,
  context_status ENUM('DRAFT', 'READY', 'STALE') NOT NULL DEFAULT 'DRAFT',
  status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_products_teacher_status (teacher_id, status),
  CONSTRAINT fk_products_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attraction_speeches (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  attraction_name VARCHAR(128) NOT NULL,
  geography_type VARCHAR(64) NULL,
  culture_tags VARCHAR(255) NULL,
  customer_experience_level VARCHAR(64) NULL,
  customer_scene TEXT NULL,
  selling_goal TEXT NULL,
  attraction_basic_info TEXT NULL,
  course_context TEXT NULL,
  speech_content TEXT NULL,
  tag_status ENUM('PENDING', 'TAGGED') NOT NULL DEFAULT 'PENDING',
  speech_status ENUM('PENDING', 'GENERATED', 'STALE') NOT NULL DEFAULT 'PENDING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_attraction_speeches_teacher (teacher_id),
  CONSTRAINT fk_attraction_speeches_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS customer_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  profile_type VARCHAR(64) NULL,
  profile_content TEXT NULL,
  shared_by VARCHAR(255) NULL,
  status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_customer_profiles_teacher_status (teacher_id, status),
  CONSTRAINT fk_customer_profiles_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_scenarios (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  training_goal VARCHAR(255) NULL,
  difficulty ENUM('入门', '进阶', '挑战') NOT NULL DEFAULT '入门',
  coaching_mode ENUM('少提示', '即时提示', '复盘提示') NOT NULL DEFAULT '即时提示',
  description TEXT NULL,
  status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_training_scenarios_teacher_status (teacher_id, status),
  CONSTRAINT fk_training_scenarios_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
