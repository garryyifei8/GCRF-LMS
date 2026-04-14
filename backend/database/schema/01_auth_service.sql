-- =========================================
-- 认证服务数据库表结构
-- Database: auth_service
-- Description: 用户认证相关表（仅 users 表）
--
-- 注意：roles/permissions/menus 等权限模型在 system-service 的
-- 05_system_service.sql 中定义。auth-service Java 代码仅使用 users 表。
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建auth_service数据库
-- CREATE DATABASE auth_service;
-- 连接到auth_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    user_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    failed_login_count INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users(user_type);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 插入默认管理员用户（密码：admin123，已 BCrypt 加密）
INSERT INTO users (user_id, username, password, email, user_type, status) VALUES
('admin', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;
