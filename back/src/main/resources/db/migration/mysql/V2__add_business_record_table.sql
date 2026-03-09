CREATE TABLE IF NOT EXISTS business_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id VARCHAR(64) NOT NULL,
  payload VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_business_record_business_id (business_id)
);
