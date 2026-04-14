-- =========================================
-- V001: Baseline migration for system-service
-- Database: system_service
-- Description: Creates all system management tables with default data
-- =========================================

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 部门表 (树形结构)
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    dept_code VARCHAR(50) UNIQUE NOT NULL,
    dept_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    dept_level INT DEFAULT 1,
    dept_path VARCHAR(500),
    leader_id BIGINT,
    leader_name VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    sort_order INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 2. 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    config_group VARCHAR(50),
    description VARCHAR(500),
    is_system BOOLEAN DEFAULT FALSE,
    is_encrypted BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 系统参数历史表
CREATE TABLE IF NOT EXISTS config_histories (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES system_configs(id) ON DELETE CASCADE,
    old_value TEXT,
    new_value TEXT,
    operator_id BIGINT,
    operator_name VARCHAR(100),
    change_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 数据字典表
CREATE TABLE IF NOT EXISTS dict_types (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(50) UNIQUE NOT NULL,
    dict_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. 数据字典项表
CREATE TABLE IF NOT EXISTS dict_items (
    id BIGSERIAL PRIMARY KEY,
    dict_type_id BIGINT NOT NULL REFERENCES dict_types(id) ON DELETE CASCADE,
    item_label VARCHAR(100) NOT NULL,
    item_value VARCHAR(100) NOT NULL,
    item_type VARCHAR(50) DEFAULT 'STRING',
    sort_order INT DEFAULT 0,
    is_default BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 角色表 (roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_desc VARCHAR(500),
    data_scope VARCHAR(20) NOT NULL DEFAULT 'ALL',
    sort_order INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_role_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_role_data_scope CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_CHILD', 'CUSTOM'))
);

-- 7. 权限表 (permissions)
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL DEFAULT 'API',
    resource_path VARCHAR(200),
    http_method VARCHAR(10),
    permission_group VARCHAR(50),
    sort_order INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_permission_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_permission_resource_type CHECK (resource_type IN ('API', 'MENU', 'BUTTON')),
    CONSTRAINT chk_permission_http_method CHECK (http_method IS NULL OR http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'))
);

-- 8. 角色权限关联表 (role_permissions)
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 9. 菜单表 (menus)
CREATE TABLE IF NOT EXISTS menus (
    id BIGSERIAL PRIMARY KEY,
    menu_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    path VARCHAR(200),
    component VARCHAR(200),
    redirect VARCHAR(200),
    icon VARCHAR(100),
    menu_type VARCHAR(20) NOT NULL DEFAULT 'MENU',
    sort_order INT DEFAULT 0,
    is_visible BOOLEAN DEFAULT TRUE,
    is_cache BOOLEAN DEFAULT FALSE,
    is_external BOOLEAN DEFAULT FALSE,
    permission_code VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT chk_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON')),
    CONSTRAINT chk_menu_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- 10. 角色菜单关联表 (role_menus)
CREATE TABLE IF NOT EXISTS role_menus (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- 11. 操作日志表 (operation_logs)
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    dept_name VARCHAR(100),
    operation VARCHAR(100) NOT NULL,
    operation_type VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    business_type VARCHAR(50),
    request_method VARCHAR(200) NOT NULL,
    request_url VARCHAR(500),
    http_method VARCHAR(10),
    request_params TEXT,
    response_result TEXT,
    error_msg TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    ip_address VARCHAR(50),
    location VARCHAR(200),
    user_agent VARCHAR(500),
    os_info VARCHAR(100),
    browser_info VARCHAR(100),
    execution_time INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_op_log_operation_type CHECK (operation_type IN ('CREATE', 'UPDATE', 'DELETE', 'QUERY', 'EXPORT', 'IMPORT', 'OTHER')),
    CONSTRAINT chk_op_log_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_op_log_http_method CHECK (http_method IS NULL OR http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'))
);

-- 12. 登录日志表 (login_logs)
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100),
    login_type VARCHAR(20) NOT NULL DEFAULT 'WEB',
    login_method VARCHAR(20),
    ip_address VARCHAR(50),
    location VARCHAR(200),
    browser VARCHAR(100),
    os VARCHAR(100),
    user_agent VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_msg TEXT,
    token VARCHAR(500),
    session_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_login_log_type CHECK (login_type IN ('WEB', 'MOBILE', 'API')),
    CONSTRAINT chk_login_log_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_login_log_method CHECK (login_method IS NULL OR login_method IN ('PASSWORD', 'SMS', 'WECHAT', 'QR_CODE'))
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_departments_parent_id ON departments(parent_id);
CREATE INDEX IF NOT EXISTS idx_departments_dept_code ON departments(dept_code);
CREATE INDEX IF NOT EXISTS idx_departments_status ON departments(status);
CREATE INDEX IF NOT EXISTS idx_system_configs_config_key ON system_configs(config_key);
CREATE INDEX IF NOT EXISTS idx_system_configs_config_group ON system_configs(config_group);
CREATE INDEX IF NOT EXISTS idx_config_histories_config_id ON config_histories(config_id);
CREATE INDEX IF NOT EXISTS idx_dict_items_dict_type_id ON dict_items(dict_type_id);

-- 角色表索引
CREATE INDEX IF NOT EXISTS idx_roles_role_code ON roles(role_code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_roles_status ON roles(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_roles_deleted_at ON roles(deleted_at);

-- 权限表索引
CREATE INDEX IF NOT EXISTS idx_permissions_code ON permissions(permission_code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_resource_type ON permissions(resource_type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_status ON permissions(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_group ON permissions(permission_group) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_permissions_deleted_at ON permissions(deleted_at);

-- 角色权限关联表索引
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- 菜单表索引
CREATE INDEX IF NOT EXISTS idx_menus_parent_id ON menus(parent_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_menus_status ON menus(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_menus_sort_order ON menus(sort_order) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_menus_permission_code ON menus(permission_code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_menus_deleted_at ON menus(deleted_at);

-- 角色菜单关联表索引
CREATE INDEX IF NOT EXISTS idx_role_menus_role_id ON role_menus(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menus_menu_id ON role_menus(menu_id);

-- 操作日志表索引
CREATE INDEX IF NOT EXISTS idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_logs_username ON operation_logs(username);
CREATE INDEX IF NOT EXISTS idx_operation_logs_operation_type ON operation_logs(operation_type);
CREATE INDEX IF NOT EXISTS idx_operation_logs_status ON operation_logs(status);
CREATE INDEX IF NOT EXISTS idx_operation_logs_created_at ON operation_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_operation_logs_business_type ON operation_logs(business_type);
CREATE INDEX IF NOT EXISTS idx_operation_logs_ip_address ON operation_logs(ip_address);

-- 登录日志表索引
CREATE INDEX IF NOT EXISTS idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_login_logs_username ON login_logs(username);
CREATE INDEX IF NOT EXISTS idx_login_logs_status ON login_logs(status);
CREATE INDEX IF NOT EXISTS idx_login_logs_created_at ON login_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_logs_login_type ON login_logs(login_type);

-- 插入默认部门数据
INSERT INTO departments (dept_code, dept_name, parent_id, dept_level, dept_path, sort_order, description) VALUES
('ROOT', '广州城市图书馆', NULL, 1, '/1', 0, '顶级机构'),
('ADMIN', '行政管理部', 1, 2, '/1/2', 1, '负责行政管理工作'),
('TECH', '技术服务部', 1, 2, '/1/3', 2, '负责图书馆技术服务'),
('CIRCULATION', '流通部', 1, 2, '/1/4', 3, '负责图书借还业务'),
('CATALOG', '编目部', 1, 2, '/1/5', 4, '负责图书编目工作'),
('REFERENCE', '参考咨询部', 1, 2, '/1/6', 5, '负责读者咨询服务')
ON CONFLICT (dept_code) DO NOTHING;

-- 插入默认系统配置
INSERT INTO system_configs (config_key, config_value, config_type, config_group, description, is_system) VALUES
('system.name', '广州城市图书馆管理系统', 'STRING', 'SYSTEM', '系统名称', TRUE),
('system.version', '1.0.0', 'STRING', 'SYSTEM', '系统版本', TRUE),
('library.name', '广州城市图书馆', 'STRING', 'LIBRARY', '图书馆名称', FALSE),
('library.address', '广州市天河区珠江新城', 'STRING', 'LIBRARY', '图书馆地址', FALSE),
('library.phone', '020-12345678', 'STRING', 'LIBRARY', '图书馆电话', FALSE),
('library.opening_hours', '周一至周日 9:00-21:00', 'STRING', 'LIBRARY', '开放时间', FALSE),
('borrow.max_books', '10', 'NUMBER', 'BORROW', '最大借阅数量', FALSE),
('borrow.max_days', '30', 'NUMBER', 'BORROW', '最大借阅天数', FALSE),
('borrow.overdue_fine_per_day', '0.10', 'NUMBER', 'BORROW', '每日逾期罚金(元)', FALSE),
('reader.default_credit_score', '100', 'NUMBER', 'READER', '默认信用分', FALSE)
ON CONFLICT (config_key) DO NOTHING;

-- 插入默认数据字典类型
INSERT INTO dict_types (dict_code, dict_name, description) VALUES
('reader_type', '读者类型', '读者的分类'),
('book_status', '图书状态', '图书的状态'),
('gender', '性别', '性别分类'),
('card_status', '借书卡状态', '借书卡的状态')
ON CONFLICT (dict_code) DO NOTHING;

-- 插入默认数据字典项
INSERT INTO dict_items (dict_type_id, item_label, item_value, sort_order) VALUES
((SELECT id FROM dict_types WHERE dict_code = 'reader_type'), '学生', 'STUDENT', 1),
((SELECT id FROM dict_types WHERE dict_code = 'reader_type'), '教师', 'TEACHER', 2),
((SELECT id FROM dict_types WHERE dict_code = 'reader_type'), '职工', 'STAFF', 3),
((SELECT id FROM dict_types WHERE dict_code = 'reader_type'), '公众', 'PUBLIC', 4),
((SELECT id FROM dict_types WHERE dict_code = 'book_status'), '可借', 'AVAILABLE', 1),
((SELECT id FROM dict_types WHERE dict_code = 'book_status'), '已借出', 'BORROWED', 2),
((SELECT id FROM dict_types WHERE dict_code = 'book_status'), '维修中', 'REPAIRING', 3),
((SELECT id FROM dict_types WHERE dict_code = 'book_status'), '丢失', 'LOST', 4),
((SELECT id FROM dict_types WHERE dict_code = 'gender'), '男', 'MALE', 1),
((SELECT id FROM dict_types WHERE dict_code = 'gender'), '女', 'FEMALE', 2),
((SELECT id FROM dict_types WHERE dict_code = 'gender'), '其他', 'OTHER', 3),
((SELECT id FROM dict_types WHERE dict_code = 'card_status'), '待激活', 'PENDING', 1),
((SELECT id FROM dict_types WHERE dict_code = 'card_status'), '正常', 'ACTIVE', 2),
((SELECT id FROM dict_types WHERE dict_code = 'card_status'), '挂失', 'SUSPENDED', 3),
((SELECT id FROM dict_types WHERE dict_code = 'card_status'), '过期', 'EXPIRED', 4),
((SELECT id FROM dict_types WHERE dict_code = 'card_status'), '注销', 'CANCELLED', 5)
ON CONFLICT DO NOTHING;

-- 插入默认角色
INSERT INTO roles (role_code, role_name, role_desc, data_scope, sort_order, status) VALUES
('SUPER_ADMIN', '超级管理员', '拥有系统所有权限', 'ALL', 1, 'ACTIVE'),
('ADMIN', '管理员', '拥有大部分管理权限', 'ALL', 2, 'ACTIVE'),
('LIBRARIAN', '图书管理员', '图书馆日常管理权限', 'DEPT', 3, 'ACTIVE'),
('USER', '普通用户', '基本查询和借阅权限', 'CUSTOM', 4, 'ACTIVE')
ON CONFLICT (role_code) DO NOTHING;

-- 插入默认权限
INSERT INTO permissions (permission_code, permission_name, resource_type, resource_path, http_method, permission_group, sort_order, status) VALUES
-- 系统管理权限
('system:user:list', '查看用户列表', 'API', '/api/v1/users', 'GET', 'system', 1, 'ACTIVE'),
('system:user:create', '创建用户', 'API', '/api/v1/users', 'POST', 'system', 2, 'ACTIVE'),
('system:user:update', '修改用户', 'API', '/api/v1/users/*', 'PUT', 'system', 3, 'ACTIVE'),
('system:user:delete', '删除用户', 'API', '/api/v1/users/*', 'DELETE', 'system', 4, 'ACTIVE'),
('system:role:list', '查看角色列表', 'API', '/api/v1/roles', 'GET', 'system', 5, 'ACTIVE'),
('system:role:create', '创建角色', 'API', '/api/v1/roles', 'POST', 'system', 6, 'ACTIVE'),
('system:role:update', '修改角色', 'API', '/api/v1/roles/*', 'PUT', 'system', 7, 'ACTIVE'),
('system:role:delete', '删除角色', 'API', '/api/v1/roles/*', 'DELETE', 'system', 8, 'ACTIVE'),
('system:dept:list', '查看部门列表', 'API', '/api/v1/departments', 'GET', 'system', 9, 'ACTIVE'),
('system:dept:create', '创建部门', 'API', '/api/v1/departments', 'POST', 'system', 10, 'ACTIVE'),
-- 图书管理权限
('book:list', '查看图书列表', 'API', '/api/v1/books', 'GET', 'book', 11, 'ACTIVE'),
('book:create', '添加图书', 'API', '/api/v1/books', 'POST', 'book', 12, 'ACTIVE'),
('book:update', '修改图书', 'API', '/api/v1/books/*', 'PUT', 'book', 13, 'ACTIVE'),
('book:delete', '删除图书', 'API', '/api/v1/books/*', 'DELETE', 'book', 14, 'ACTIVE'),
-- 读者管理权限
('reader:list', '查看读者列表', 'API', '/api/v1/readers', 'GET', 'reader', 15, 'ACTIVE'),
('reader:create', '添加读者', 'API', '/api/v1/readers', 'POST', 'reader', 16, 'ACTIVE'),
('reader:update', '修改读者', 'API', '/api/v1/readers/*', 'PUT', 'reader', 17, 'ACTIVE'),
-- 借阅管理权限
('circulation:borrow', '借书', 'API', '/api/v1/borrows/borrow', 'POST', 'circulation', 21, 'ACTIVE'),
('circulation:return', '还书', 'API', '/api/v1/borrows/return', 'POST', 'circulation', 22, 'ACTIVE'),
('circulation:renew', '续借', 'API', '/api/v1/borrows/renew', 'POST', 'circulation', 23, 'ACTIVE'),
('circulation:list', '查看借阅记录', 'API', '/api/v1/borrows', 'GET', 'circulation', 24, 'ACTIVE'),
('circulation:reserve', '预约图书', 'API', '/api/v1/reserves/reserve', 'POST', 'circulation', 25, 'ACTIVE')
ON CONFLICT (permission_code) DO NOTHING;

-- 插入默认菜单
INSERT INTO menus (menu_name, parent_id, path, component, icon, menu_type, sort_order, permission_code, status) VALUES
('系统管理', NULL, '/system', NULL, 'setting', 'DIR', 1, NULL, 'ACTIVE'),
('用户管理', 1, '/system/users', 'system/users/index', 'user', 'MENU', 1, 'system:user:list', 'ACTIVE'),
('角色管理', 1, '/system/roles', 'system/roles/index', 'role', 'MENU', 2, 'system:role:list', 'ACTIVE'),
('部门管理', 1, '/system/departments', 'system/departments/index', 'dept', 'MENU', 3, 'system:dept:list', 'ACTIVE'),
('菜单管理', 1, '/system/menus', 'system/menus/index', 'menu', 'MENU', 4, NULL, 'ACTIVE'),
('图书管理', NULL, '/book', NULL, 'book', 'DIR', 2, NULL, 'ACTIVE'),
('图书列表', 6, '/book/list', 'book/list/index', 'list', 'MENU', 1, 'book:list', 'ACTIVE'),
('读者管理', NULL, '/reader', NULL, 'reader', 'DIR', 3, NULL, 'ACTIVE'),
('读者列表', 8, '/reader/list', 'reader/list/index', 'list', 'MENU', 1, 'reader:list', 'ACTIVE'),
('流通管理', NULL, '/circulation', NULL, 'circulation', 'DIR', 4, NULL, 'ACTIVE'),
('借阅管理', 10, '/circulation/borrow', 'circulation/borrow/index', 'borrow', 'MENU', 1, 'circulation:list', 'ACTIVE'),
('预约管理', 10, '/circulation/reserve', 'circulation/reserve/index', 'reserve', 'MENU', 2, NULL, 'ACTIVE'),
('系统监控', NULL, '/monitor', NULL, 'monitor', 'DIR', 5, NULL, 'ACTIVE'),
('操作日志', 13, '/monitor/operation-logs', 'monitor/operation-logs/index', 'log', 'MENU', 1, NULL, 'ACTIVE'),
('登录日志', 13, '/monitor/login-logs', 'monitor/login-logs/index', 'login', 'MENU', 2, NULL, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 为超级管理员分配所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN'),
    id
FROM permissions
WHERE deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 为超级管理员分配所有菜单
INSERT INTO role_menus (role_id, menu_id)
SELECT
    (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN'),
    id
FROM menus
WHERE deleted_at IS NULL
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 为管理员分配部分权限 (图书、读者、流通)
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM roles WHERE role_code = 'ADMIN'),
    id
FROM permissions
WHERE permission_group IN ('book', 'reader', 'circulation')
AND deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;
