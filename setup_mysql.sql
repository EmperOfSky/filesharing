-- ========================================
-- 文件共享系统 - MySQL初始化脚本
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS filesharing 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE filesharing;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `password` VARCHAR(255) NOT NULL COMMENT '密码密文',
  nickname VARCHAR(50) COMMENT '昵称',
  avatar VARCHAR(255) COMMENT '头像URL',
  storage_quota BIGINT DEFAULT 1073741824 COMMENT '存储配额(字节)',
  used_storage BIGINT DEFAULT 0 COMMENT '已使用存储(字节)',
  `status` ENUM('ACTIVE', 'DISABLED', 'SUSPENDED') DEFAULT 'ACTIVE' COMMENT '账户状态',
  `role` ENUM('USER', 'ADMIN') DEFAULT 'USER' COMMENT '用户角色',
  last_login_time DATETIME COMMENT '最后登录时间',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_username (username),
  KEY idx_email (email),
  KEY idx_status (`status`),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建文件夹表
CREATE TABLE IF NOT EXISTS folders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件夹ID',
  folder_name VARCHAR(255) NOT NULL COMMENT '文件夹名称',
  folder_path VARCHAR(1024) COMMENT '文件夹路径',
  parent_id BIGINT COMMENT '父文件夹ID',
  owner_id BIGINT NOT NULL COMMENT '所有者ID',
  `description` TEXT COMMENT '描述',
  is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
  folder_size BIGINT DEFAULT 0 COMMENT '文件夹大小',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE CASCADE,
  KEY idx_owner_id (owner_id),
  KEY idx_parent_id (parent_id),
  KEY idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件夹表';

-- 创建文件表
CREATE TABLE IF NOT EXISTS files (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
  storage_name VARCHAR(255) NOT NULL UNIQUE COMMENT '存储名称',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_path VARCHAR(1024) COMMENT '文件路径',
  file_size BIGINT NOT NULL COMMENT '文件大小',
  content_type VARCHAR(100) COMMENT '文件类型',
  extension VARCHAR(20) COMMENT '文件扩展名',
  md5_hash VARCHAR(32) COMMENT 'MD5哈希',
  uploader_id BIGINT NOT NULL COMMENT '上传者ID',
  folder_id BIGINT COMMENT '所在文件夹ID',
  `status` ENUM('AVAILABLE', 'DELETED', 'ARCHIVED', 'QUARANTINED') DEFAULT 'AVAILABLE' COMMENT '文件状态',
  is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  preview_count INT DEFAULT 0 COMMENT '预览次数',
  share_count INT DEFAULT 0 COMMENT '分享次数',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL,
  KEY idx_uploader_id (uploader_id),
  KEY idx_folder_id (folder_id),
  KEY idx_status (`status`),
  KEY idx_is_public (is_public),
  KEY idx_created_at (created_at),
  KEY idx_md5_hash (md5_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

-- 创建文件版本表
CREATE TABLE IF NOT EXISTS file_versions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  version_number INT NOT NULL COMMENT '版本号',
  storage_name VARCHAR(255) NOT NULL COMMENT '存储名称',
  file_size BIGINT COMMENT '文件大小',
  uploader_id BIGINT COMMENT '上传者ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL,
  UNIQUE KEY uk_file_version (file_id, version_number),
  KEY idx_file_id (file_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件版本表';

-- 创建文件标签表
CREATE TABLE IF NOT EXISTS file_tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  UNIQUE KEY uk_file_tag (file_id, tag_name),
  KEY idx_file_id (file_id),
  KEY idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件标签表';

-- 创建分享记录表
CREATE TABLE IF NOT EXISTS shares (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分享ID',
  file_id BIGINT COMMENT '文件ID',
  folder_id BIGINT COMMENT '文件夹ID',
  share_key VARCHAR(50) NOT NULL UNIQUE COMMENT '分享密钥',
  sharer_id BIGINT NOT NULL COMMENT '分享者ID',
  share_type ENUM('PUBLIC', 'PRIVATE', 'PROTECTED') DEFAULT 'PUBLIC' COMMENT '分享类型',
  expiration_time DATETIME COMMENT '过期时间',
  access_count INT DEFAULT 0 COMMENT '访问次数',
  max_access_count INT COMMENT '最大访问次数',
  `password` VARCHAR(100) COMMENT '访问密码',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
  FOREIGN KEY (sharer_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_share_key (share_key),
  KEY idx_sharer_id (sharer_id),
  KEY idx_expiration_time (expiration_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享记录表';

-- 创建文件统计表
CREATE TABLE IF NOT EXISTS file_statistics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  preview_count INT DEFAULT 0 COMMENT '预览次数',
  share_count INT DEFAULT 0 COMMENT '分享次数',
  last_accessed DATETIME COMMENT '最后访问时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  UNIQUE KEY uk_file_id (file_id),
  KEY idx_download_count (download_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件统计表';

-- 创建分块上传记录表
CREATE TABLE IF NOT EXISTS chunk_upload_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
  upload_id VARCHAR(50) NOT NULL UNIQUE COMMENT '上传会话ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  total_chunks INT NOT NULL COMMENT '总分块数',
  uploaded_chunks INT DEFAULT 0 COMMENT '已上传分块数',
  file_size BIGINT NOT NULL COMMENT '文件总大小',
  uploader_id BIGINT NOT NULL COMMENT '上传者ID',
  `status` ENUM('UPLOADING', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'UPLOADING' COMMENT '上传状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_upload_id (upload_id),
  KEY idx_uploader_id (uploader_id),
  KEY idx_status (`status`),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分块上传记录表';

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  user_id BIGINT COMMENT '用户ID',
  operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
  resource_type VARCHAR(50) COMMENT '资源类型',
  resource_id VARCHAR(50) COMMENT '资源ID',
  operation_details TEXT COMMENT '操作详情',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  KEY idx_user_id (user_id),
  KEY idx_operation_type (operation_type),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 创建通知表
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
  user_id BIGINT NOT NULL COMMENT '接收者ID',
  notification_type VARCHAR(50) COMMENT '通知类型',
  title VARCHAR(255) COMMENT '通知标题',
  `message` TEXT COMMENT '通知内容',
  is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_user_id (user_id),
  KEY idx_is_read (is_read),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 创建批量操作表
CREATE TABLE IF NOT EXISTS batch_operations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '批量操作ID',
  batch_name VARCHAR(255) NOT NULL COMMENT '批量操作名称',
  operation_type VARCHAR(50) COMMENT '操作类型',
  operator_id BIGINT NOT NULL COMMENT '操作者ID',
  total_items INT DEFAULT 0 COMMENT '总项目数',
  processed_items INT DEFAULT 0 COMMENT '已处理项目数',
  status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING' COMMENT '状态',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE CASCADE,
  KEY idx_operator_id (operator_id),
  KEY idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量操作表';

-- 创建索引以优化查询
CREATE INDEX idx_files_uploader_created ON files(uploader_id, created_at);
CREATE INDEX idx_files_folder_created ON files(folder_id, created_at);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 设置自增ID起始值
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE folders AUTO_INCREMENT = 1;
ALTER TABLE files AUTO_INCREMENT = 1;

-- 统计表大小
SELECT
    TABLE_NAME,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS 'Size (MB)'
FROM
    information_schema.TABLES
WHERE
    TABLE_SCHEMA = 'filesharing'
ORDER BY
    (DATA_LENGTH + INDEX_LENGTH) DESC;
