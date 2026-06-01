USE teach_agent;

ALTER TABLE products
  ADD COLUMN change_summary TEXT NULL AFTER product_context,
  ADD COLUMN change_status ENUM('PENDING', 'GENERATED', 'FAILED') NOT NULL DEFAULT 'PENDING' AFTER change_summary;
