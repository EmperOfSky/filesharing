-- 创建数据库
CREATE DATABASE IF NOT EXISTS filesharing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'filesharing_user'@'localhost' IDENTIFIED BY 'filesharing_password_123';
CREATE USER IF NOT EXISTS 'filesharing_user'@'%' IDENTIFIED BY 'filesharing_password_123';

-- 授予权限
GRANT ALL PRIVILEGES ON filesharing.* TO 'filesharing_user'@'localhost';
GRANT ALL PRIVILEGES ON filesharing.* TO 'filesharing_user'@'%';

FLUSH PRIVILEGES;

-- 选择数据库
USE filesharing;

-- 确保所有表都使用UTF8MB4编码
ALTER DATABASE filesharing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
