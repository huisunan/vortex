CREATE TABLE IF NOT EXISTS trace_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(128) NOT NULL,
  business_id VARCHAR(64) NOT NULL,
  node_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  detail VARCHAR(1024) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_trace_business_id (business_id)
);
