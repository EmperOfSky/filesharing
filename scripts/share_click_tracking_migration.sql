-- 短链接点击追踪迁移脚本（MySQL 8+）

ALTER TABLE share_records
    ADD COLUMN IF NOT EXISTS pv_count INT DEFAULT 0 COMMENT '短链接总点击量(PV)',
    ADD COLUMN IF NOT EXISTS uv_count INT DEFAULT 0 COMMENT '短链接独立访客数(UV)',
    ADD COLUMN IF NOT EXISTS last_visitor_ip VARCHAR(64) NULL COMMENT '最近访客IP',
    ADD COLUMN IF NOT EXISTS last_visitor_address VARCHAR(255) NULL COMMENT '最近访客地址',
    ADD COLUMN IF NOT EXISTS last_access_at DATETIME NULL COMMENT '最近点击时间';

CREATE TABLE IF NOT EXISTS share_click_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    share_id BIGINT NOT NULL COMMENT '分享记录ID',
    share_key VARCHAR(32) NOT NULL COMMENT '短链接key',
    visitor_ip VARCHAR(64) NULL COMMENT '访客IP',
    visitor_address VARCHAR(255) NULL COMMENT '访客地址',
    visitor_fingerprint VARCHAR(64) NOT NULL COMMENT '访客指纹',
    user_agent VARCHAR(500) NULL COMMENT 'User-Agent',
    referer VARCHAR(255) NULL COMMENT '访问来源',
    accessed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    CONSTRAINT fk_share_click_logs_share_id FOREIGN KEY (share_id) REFERENCES share_records(id) ON DELETE CASCADE,
    KEY idx_share_click_logs_share_id (share_id),
    KEY idx_share_click_logs_share_key (share_key),
    KEY idx_share_click_logs_accessed_at (accessed_at),
    KEY idx_share_click_logs_fingerprint (share_id, visitor_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短链接点击日志';

ALTER TABLE share_click_logs
    ADD COLUMN IF NOT EXISTS referer VARCHAR(255) NULL COMMENT '访问来源';
