USE teach_agent;

CREATE TABLE IF NOT EXISTS workflow_configs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  workflow_key VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  base_url VARCHAR(255) NOT NULL DEFAULT 'https://api.dify.ai/v1',
  workflow_id VARCHAR(128) NOT NULL,
  api_key_ciphertext TEXT NULL,
  api_key_last4 VARCHAR(8) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  last_test_status ENUM('UNTESTED', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'UNTESTED',
  last_test_message VARCHAR(512) NULL,
  last_test_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_workflow_configs_teacher_key (teacher_id, workflow_key),
  KEY idx_workflow_configs_teacher_enabled (teacher_id, enabled),
  CONSTRAINT fk_workflow_configs_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
