USE teach_agent;

CREATE TABLE IF NOT EXISTS agent_workflows (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_id BIGINT NOT NULL,
  workflow_key VARCHAR(64) NOT NULL,
  workflow_config_id BIGINT NULL,
  custom_workflow_id VARCHAR(128) NULL,
  custom_api_key_ciphertext TEXT NULL,
  custom_api_key_last4 VARCHAR(8) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_agent_workflows_agent_key (agent_id, workflow_key),
  KEY idx_agent_workflows_config (workflow_config_id),
  CONSTRAINT fk_agent_workflows_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
  CONSTRAINT fk_agent_workflows_config FOREIGN KEY (workflow_config_id) REFERENCES workflow_configs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
