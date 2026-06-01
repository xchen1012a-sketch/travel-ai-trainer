CREATE DATABASE IF NOT EXISTS teach_agent
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE teach_agent;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  email VARCHAR(128) NULL,
  phone VARCHAR(32) NULL,
  avatar_url VARCHAR(512) NULL,
  status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  subject VARCHAR(64) NULL,
  grade_level VARCHAR(64) NULL,
  description TEXT NULL,
  status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_courses_teacher_status (teacher_id, status),
  CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE agents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT NULL,
  avatar_url VARCHAR(512) NULL,
  system_prompt TEXT NULL,
  opening_message TEXT NULL,
  dify_app_id VARCHAR(128) NULL,
  dify_workflow_id VARCHAR(128) NULL,
  dify_api_key_ref VARCHAR(128) NULL,
  status ENUM('DRAFT', 'PUBLISHED', 'DISABLED') NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_agents_teacher_status (teacher_id, status),
  KEY idx_agents_course (course_id),
  CONSTRAINT fk_agents_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
  CONSTRAINT fk_agents_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE knowledge_bases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NULL,
  agent_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT NULL,
  dify_dataset_id VARCHAR(128) NULL,
  status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_knowledge_bases_teacher_status (teacher_id, status),
  KEY idx_knowledge_bases_course (course_id),
  KEY idx_knowledge_bases_agent (agent_id),
  CONSTRAINT fk_knowledge_bases_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
  CONSTRAINT fk_knowledge_bases_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_knowledge_bases_agent FOREIGN KEY (agent_id) REFERENCES agents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE knowledge_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  knowledge_base_id BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(512) NOT NULL,
  mime_type VARCHAR(128) NULL,
  file_size BIGINT NOT NULL DEFAULT 0,
  dify_document_id VARCHAR(128) NULL,
  parse_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
  parse_error TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_knowledge_files_kb_status (knowledge_base_id, parse_status),
  KEY idx_knowledge_files_uploader (uploader_id),
  CONSTRAINT fk_knowledge_files_kb FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases (id),
  CONSTRAINT fk_knowledge_files_uploader FOREIGN KEY (uploader_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE classes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NULL,
  agent_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  class_code VARCHAR(16) NOT NULL,
  description TEXT NULL,
  join_enabled TINYINT(1) NOT NULL DEFAULT 1,
  status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  UNIQUE KEY uk_classes_class_code (class_code),
  KEY idx_classes_teacher_status (teacher_id, status),
  KEY idx_classes_course (course_id),
  KEY idx_classes_agent (agent_id),
  CONSTRAINT fk_classes_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
  CONSTRAINT fk_classes_course FOREIGN KEY (course_id) REFERENCES courses (id),
  CONSTRAINT fk_classes_agent FOREIGN KEY (agent_id) REFERENCES agents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE class_members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  nickname VARCHAR(64) NULL,
  status ENUM('ACTIVE', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  removed_at DATETIME NULL,
  UNIQUE KEY uk_class_members_class_student (class_id, student_id),
  KEY idx_class_members_student_status (student_id, status),
  CONSTRAINT fk_class_members_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_class_members_student FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE conversations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  class_id BIGINT NOT NULL,
  agent_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  title VARCHAR(255) NULL,
  dify_conversation_id VARCHAR(128) NULL,
  status ENUM('OPEN', 'CLOSED', 'FLAGGED') NOT NULL DEFAULT 'OPEN',
  last_message_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_conversations_class_student (class_id, student_id),
  KEY idx_conversations_agent_status (agent_id, status),
  KEY idx_conversations_last_message (last_message_at),
  CONSTRAINT fk_conversations_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_conversations_agent FOREIGN KEY (agent_id) REFERENCES agents (id),
  CONSTRAINT fk_conversations_student FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  sender_type ENUM('STUDENT', 'AGENT', 'TEACHER', 'SYSTEM') NOT NULL,
  sender_id BIGINT NULL,
  content MEDIUMTEXT NOT NULL,
  content_type ENUM('TEXT', 'MARKDOWN', 'IMAGE', 'FILE') NOT NULL DEFAULT 'TEXT',
  dify_message_id VARCHAR(128) NULL,
  dify_task_id VARCHAR(128) NULL,
  token_count INT NULL,
  latency_ms INT NULL,
  error_code VARCHAR(64) NULL,
  error_message TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_messages_conversation_created (conversation_id, created_at),
  KEY idx_messages_sender (sender_type, sender_id),
  CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interaction_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  agent_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  instruction TEXT NOT NULL,
  due_at DATETIME NULL,
  status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_interaction_tasks_class_status (class_id, status),
  KEY idx_interaction_tasks_teacher_status (teacher_id, status),
  CONSTRAINT fk_interaction_tasks_teacher FOREIGN KEY (teacher_id) REFERENCES users (id),
  CONSTRAINT fk_interaction_tasks_class FOREIGN KEY (class_id) REFERENCES classes (id),
  CONSTRAINT fk_interaction_tasks_agent FOREIGN KEY (agent_id) REFERENCES agents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE workflow_configs (
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

CREATE TABLE agent_workflows (
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

CREATE TABLE task_submissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  conversation_id BIGINT NULL,
  status ENUM('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'REVIEWED') NOT NULL DEFAULT 'NOT_STARTED',
  submitted_at DATETIME NULL,
  teacher_comment TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task_submissions_task_student (task_id, student_id),
  KEY idx_task_submissions_student_status (student_id, status),
  CONSTRAINT fk_task_submissions_task FOREIGN KEY (task_id) REFERENCES interaction_tasks (id),
  CONSTRAINT fk_task_submissions_student FOREIGN KEY (student_id) REFERENCES users (id),
  CONSTRAINT fk_task_submissions_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL,
  message_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  rating TINYINT NULL,
  content TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_feedback_conversation (conversation_id),
  KEY idx_feedback_message (message_id),
  KEY idx_feedback_user (user_id),
  CONSTRAINT fk_feedback_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
  CONSTRAINT fk_feedback_message FOREIGN KEY (message_id) REFERENCES messages (id),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT chk_feedback_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_id BIGINT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id BIGINT NULL,
  ip_address VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  detail JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_logs_actor_created (actor_id, created_at),
  KEY idx_audit_logs_target (target_type, target_id),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  route_name VARCHAR(128) NULL,
  old_info TEXT NULL,
  new_info TEXT NULL,
  product_context TEXT NULL,
  change_summary TEXT NULL,
  change_status ENUM('PENDING', 'GENERATED', 'FAILED') NOT NULL DEFAULT 'PENDING',
  context_status ENUM('DRAFT', 'READY', 'STALE') NOT NULL DEFAULT 'DRAFT',
  status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  KEY idx_products_teacher_status (teacher_id, status),
  CONSTRAINT fk_products_teacher FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attraction_speeches (
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

CREATE TABLE customer_profiles (
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

CREATE TABLE training_scenarios (
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
