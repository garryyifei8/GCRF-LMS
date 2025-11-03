-- =========================================
-- 认证服务数据库表结构
-- Database: auth_service
-- Description: 用户认证、权限管理相关表
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建auth_service数据库
-- CREATE DATABASE auth_service;
-- 连接到auth_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) UNIQUE NOT NULL -- '用户ID(学号/工号)',
    username VARCHAR(100) NOT NULL -- '用户名',
    password VARCHAR(255) NOT NULL -- '密码(BCrypt加密)',
    email VARCHAR(100) UNIQUE -- '邮箱',
    phone VARCHAR(20) -- '手机号',
    user_type VARCHAR(20) NOT NULL DEFAULT 'STUDENT' -- '用户类型: STUDENT/TEACHER/ADMIN',
    avatar_url VARCHAR(500) -- '头像URL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' -- '状态: ACTIVE/INACTIVE/LOCKED',
    last_login_time TIMESTAMP -- '最后登录时间',
    last_login_ip VARCHAR(50) -- '最后登录IP',
    failed_login_count INT DEFAULT 0 -- '登录失败次数',
    locked_until TIMESTAMP -- '锁定截止时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- '软删除时间'
);

-- 2. 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE NOT NULL -- '角色编码',
    role_name VARCHAR(100) NOT NULL -- '角色名称',
    description VARCHAR(500) -- '角色描述',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' -- '状态: ACTIVE/INACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) UNIQUE NOT NULL -- '权限编码',
    permission_name VARCHAR(100) NOT NULL -- '权限名称',
    resource_type VARCHAR(50) -- '资源类型: MENU/BUTTON/API',
    resource_path VARCHAR(200) -- '资源路径',
    description VARCHAR(500) -- '权限描述',
    parent_id BIGINT -- '父权限ID',
    sort_order INT DEFAULT 0 -- '排序',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);

-- 5. 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

-- 6. 登录日志表
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    login_type VARCHAR(20) NOT NULL DEFAULT 'PASSWORD' -- '登录方式: PASSWORD/FACE/WECHAT',
    login_status VARCHAR(20) NOT NULL -- '登录状态: SUCCESS/FAILED',
    login_ip VARCHAR(50) -- '登录IP',
    login_device VARCHAR(200) -- '登录设备',
    login_location VARCHAR(200) -- '登录地点',
    failure_reason VARCHAR(500) -- '失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_users_user_id ON users(user_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX idx_login_logs_created_at ON login_logs(created_at);

-- 插入默认角色数据
INSERT INTO roles (role_code, role_name, description) VALUES
('ROLE_ADMIN', '系统管理员', '拥有系统全部权限'),
('ROLE_LIBRARIAN', '图书管理员', '负责图书管理、编目、流通等业务'),
('ROLE_OPERATOR', '业务操作员', '负责借还书、读者管理等日常业务'),
('ROLE_READER', '普通读者', '普通读者权限')
ON CONFLICT (role_code) DO NOTHING;

-- 插入默认管理员用户
INSERT INTO users (user_id, username, password, email, user_type, status) VALUES
('admin', '系统管理员', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

-- 为默认管理员分配角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.user_id = 'admin' AND r.role_code = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 添加表注释
-- ON TABLE users IS '用户表';
-- ON TABLE roles IS '角色表';
-- ON TABLE permissions IS '权限表';
-- ON TABLE user_roles IS '用户角色关联表';
-- ON TABLE role_permissions IS '角色权限关联表';
-- ON TABLE login_logs IS '登录日志表';
