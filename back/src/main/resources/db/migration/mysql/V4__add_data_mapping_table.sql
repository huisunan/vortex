-- 数据映射表：维护内部业务 ID 与外部平台 ID 的映射关系
CREATE TABLE IF NOT EXISTS data_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_id VARCHAR(128) NOT NULL COMMENT '内部业务 ID',
  platform_code VARCHAR(64) NOT NULL COMMENT '目标平台编码',
  platform_id VARCHAR(128) NULL COMMENT '平台返回的唯一 ID',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：SUCCESS/FAILED',
  error_code VARCHAR(64) NULL COMMENT '错误码',
  error_message VARCHAR(512) NULL COMMENT '错误信息',
  run_id VARCHAR(128) NOT NULL COMMENT '运行 ID',
  mapped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '映射时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_business_platform (business_id, platform_code) COMMENT '唯一键：业务 ID+ 平台编码',
  KEY idx_platform_id (platform_code, platform_id) COMMENT '索引：平台反查',
  KEY idx_run_id (run_id) COMMENT '索引：运行 ID 查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据映射表';
