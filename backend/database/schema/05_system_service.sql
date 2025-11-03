-- =========================================
-- 系统服务数据库表结构
-- Database: system_service
-- Description: 部门管理、系统配置相关表
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建system_service数据库
-- CREATE DATABASE system_service;
-- 连接到system_service数据库后执行以下脚本

-- 启用UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. 部门表 (树形结构)
CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    dept_code VARCHAR(50) UNIQUE NOT NULL, -- 部门编码
    dept_name VARCHAR(100) NOT NULL, -- 部门名称
    parent_id BIGINT, -- 父部门ID
    dept_level INT DEFAULT 1, -- 部门层级
    dept_path VARCHAR(500), -- 部门路径 (如: /1/2/3)
    leader_id BIGINT, -- 部门负责人ID(关联auth_service.users.id)
    leader_name VARCHAR(100), -- 负责人姓名(冗余字段)
    phone VARCHAR(20), -- 联系电话
    email VARCHAR(100), -- 邮箱
    sort_order INT DEFAULT 0, -- 排序
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE/INACTIVE
    description VARCHAR(500), -- 描述
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP -- 软删除时间
);

-- 2. 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL, -- 配置键
    config_value TEXT NOT NULL, -- 配置值
    config_type VARCHAR(50) NOT NULL DEFAULT 'STRING', -- 值类型: STRING/NUMBER/BOOLEAN/JSON
    config_group VARCHAR(50), -- 配置分组
    description VARCHAR(500), -- 描述
    is_system BOOLEAN DEFAULT FALSE, -- 是否系统配置(系统配置不可删除)
    is_encrypted BOOLEAN DEFAULT FALSE, -- 是否加密
    sort_order INT DEFAULT 0, -- 排序
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 系统参数历史表
CREATE TABLE IF NOT EXISTS config_histories (
    id BIGSERIAL PRIMARY KEY,
    config_id BIGINT NOT NULL REFERENCES system_configs(id) ON DELETE CASCADE,
    old_value TEXT, -- 旧值
    new_value TEXT, -- 新值
    operator_id BIGINT, -- 操作人ID(关联auth_service.users.id)
    operator_name VARCHAR(100), -- 操作人姓名
    change_reason VARCHAR(500), -- 修改原因
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 数据字典表
CREATE TABLE IF NOT EXISTS dict_types (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(50) UNIQUE NOT NULL, -- 字典编码
    dict_name VARCHAR(100) NOT NULL, -- 字典名称
    description VARCHAR(500), -- 描述
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE/INACTIVE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. 数据字典项表
CREATE TABLE IF NOT EXISTS dict_items (
    id BIGSERIAL PRIMARY KEY,
    dict_type_id BIGINT NOT NULL REFERENCES dict_types(id) ON DELETE CASCADE,
    item_label VARCHAR(100) NOT NULL, -- 字典项标签
    item_value VARCHAR(100) NOT NULL, -- 字典项值
    item_type VARCHAR(50) DEFAULT 'STRING', -- 值类型
    sort_order INT DEFAULT 0, -- 排序
    is_default BOOLEAN DEFAULT FALSE, -- 是否默认
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE/INACTIVE
    description VARCHAR(500), -- 描述
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. 角色表 (roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) UNIQUE NOT NULL, -- 角色编码
    role_name VARCHAR(100) NOT NULL, -- 角色名称
    role_desc VARCHAR(500), -- 角色描述
    data_scope VARCHAR(20) NOT NULL DEFAULT 'ALL', -- 数据范围: ALL-全部, DEPT-本部门, DEPT_AND_CHILD-本部门及子部门, CUSTOM-自定义
    sort_order INT DEFAULT 0, -- 显示顺序
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE-正常, DISABLED-停用
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- 软删除标记
    CONSTRAINT chk_role_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_role_data_scope CHECK (data_scope IN ('ALL', 'DEPT', 'DEPT_AND_CHILD', 'CUSTOM'))
);

-- 7. 权限表 (permissions)
CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(100) UNIQUE NOT NULL, -- 权限编码 (格式: system:user:list)
    permission_name VARCHAR(100) NOT NULL, -- 权限名称
    resource_type VARCHAR(20) NOT NULL DEFAULT 'API', -- 资源类型: API-接口, MENU-菜单, BUTTON-按钮
    resource_path VARCHAR(200), -- 资源路径 (API路径或菜单路径)
    http_method VARCHAR(10), -- HTTP方法: GET, POST, PUT, DELETE
    permission_group VARCHAR(50), -- 权限分组
    sort_order INT DEFAULT 0, -- 显示顺序
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE-正常, DISABLED-停用
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- 软删除标记
    CONSTRAINT chk_permission_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_permission_resource_type CHECK (resource_type IN ('API', 'MENU', 'BUTTON')),
    CONSTRAINT chk_permission_http_method CHECK (http_method IS NULL OR http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'))
);

-- 8. 角色权限关联表 (role_permissions)
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL, -- 角色ID
    permission_id BIGINT NOT NULL, -- 权限ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 9. 菜单表 (menus)
CREATE TABLE IF NOT EXISTS menus (
    id BIGSERIAL PRIMARY KEY,
    menu_name VARCHAR(100) NOT NULL, -- 菜单名称
    parent_id BIGINT, -- 父菜单ID (NULL=根菜单)
    path VARCHAR(200), -- 路由路径
    component VARCHAR(200), -- 组件路径
    redirect VARCHAR(200), -- 重定向路径
    icon VARCHAR(100), -- 菜单图标
    menu_type VARCHAR(20) NOT NULL DEFAULT 'MENU', -- 菜单类型: DIR-目录, MENU-菜单, BUTTON-按钮
    sort_order INT DEFAULT 0, -- 显示顺序
    is_visible BOOLEAN DEFAULT TRUE, -- 是否可见
    is_cache BOOLEAN DEFAULT FALSE, -- 是否缓存
    is_external BOOLEAN DEFAULT FALSE, -- 是否外链
    permission_code VARCHAR(100), -- 权限标识 (关联permissions.permission_code)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 状态: ACTIVE-正常, DISABLED-停用
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP, -- 软删除标记
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT chk_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON')),
    CONSTRAINT chk_menu_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- 10. 角色菜单关联表 (role_menus)
CREATE TABLE IF NOT EXISTS role_menus (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL, -- 角色ID
    menu_id BIGINT NOT NULL, -- 菜单ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- 11. 操作日志表 (operation_logs)
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- 用户ID
    username VARCHAR(50), -- 用户名
    dept_name VARCHAR(100), -- 部门名称
    operation VARCHAR(100) NOT NULL, -- 操作描述
    operation_type VARCHAR(20) NOT NULL DEFAULT 'OTHER', -- 操作类型: CREATE-新增, UPDATE-修改, DELETE-删除, QUERY-查询, EXPORT-导出, IMPORT-导入, OTHER-其他
    business_type VARCHAR(50), -- 业务类型
    request_method VARCHAR(200) NOT NULL, -- 请求方法 (格式: Controller.method)
    request_url VARCHAR(500), -- 请求URL
    http_method VARCHAR(10), -- HTTP方法
    request_params TEXT, -- 请求参数 (JSON格式)
    response_result TEXT, -- 响应结果 (JSON格式,可能很大,仅记录概要)
    error_msg TEXT, -- 错误信息
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- 执行状态: SUCCESS-成功, FAILURE-失败
    ip_address VARCHAR(50), -- IP地址
    location VARCHAR(200), -- 操作地点 (根据IP解析)
    user_agent VARCHAR(500), -- 浏览器User-Agent
    os_info VARCHAR(100), -- 操作系统
    browser_info VARCHAR(100), -- 浏览器信息
    execution_time INTEGER, -- 执行时长 (毫秒)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_op_log_operation_type CHECK (operation_type IN ('CREATE', 'UPDATE', 'DELETE', 'QUERY', 'EXPORT', 'IMPORT', 'OTHER')),
    CONSTRAINT chk_op_log_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_op_log_http_method CHECK (http_method IS NULL OR http_method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH'))
);

-- 12. 登录日志表 (login_logs)
CREATE TABLE IF NOT EXISTS login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- 用户ID
    username VARCHAR(50) NOT NULL, -- 用户名
    dept_name VARCHAR(100), -- 部门名称
    login_type VARCHAR(20) NOT NULL DEFAULT 'WEB', -- 登录类型: WEB-网页, MOBILE-移动端, API-API接口
    login_method VARCHAR(20), -- 登录方式: PASSWORD-密码, SMS-短信, WECHAT-微信, QR_CODE-扫码
    ip_address VARCHAR(50), -- IP地址
    location VARCHAR(200), -- 登录地点 (根据IP解析)
    browser VARCHAR(100), -- 浏览器
    os VARCHAR(100), -- 操作系统
    user_agent VARCHAR(500), -- User-Agent
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS', -- 登录状态: SUCCESS-成功, FAILURE-失败
    error_msg TEXT, -- 失败原因
    token VARCHAR(500), -- JWT Token (可选,用于追踪)
    session_id VARCHAR(100), -- 会话ID
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_login_log_type CHECK (login_type IN ('WEB', 'MOBILE', 'API')),
    CONSTRAINT chk_login_log_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_login_log_method CHECK (login_method IS NULL OR login_method IN ('PASSWORD', 'SMS', 'WECHAT', 'QR_CODE'))
);

-- 创建索引
CREATE INDEX idx_departments_parent_id ON departments(parent_id);
CREATE INDEX idx_departments_dept_code ON departments(dept_code);
CREATE INDEX idx_departments_status ON departments(status);
CREATE INDEX idx_system_configs_config_key ON system_configs(config_key);
CREATE INDEX idx_system_configs_config_group ON system_configs(config_group);
CREATE INDEX idx_config_histories_config_id ON config_histories(config_id);
CREATE INDEX idx_dict_items_dict_type_id ON dict_items(dict_type_id);

-- 角色表索引
CREATE INDEX idx_roles_role_code ON roles(role_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_roles_status ON roles(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_roles_deleted_at ON roles(deleted_at);

-- 权限表索引
CREATE INDEX idx_permissions_code ON permissions(permission_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_resource_type ON permissions(resource_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_status ON permissions(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_group ON permissions(permission_group) WHERE deleted_at IS NULL;
CREATE INDEX idx_permissions_deleted_at ON permissions(deleted_at);

-- 角色权限关联表索引
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- 菜单表索引
CREATE INDEX idx_menus_parent_id ON menus(parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_menus_status ON menus(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_menus_sort_order ON menus(sort_order) WHERE deleted_at IS NULL;
CREATE INDEX idx_menus_permission_code ON menus(permission_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_menus_deleted_at ON menus(deleted_at);

-- 角色菜单关联表索引
CREATE INDEX idx_role_menus_role_id ON role_menus(role_id);
CREATE INDEX idx_role_menus_menu_id ON role_menus(menu_id);

-- 操作日志表索引
CREATE INDEX idx_operation_logs_user_id ON operation_logs(user_id);
CREATE INDEX idx_operation_logs_username ON operation_logs(username);
CREATE INDEX idx_operation_logs_operation_type ON operation_logs(operation_type);
CREATE INDEX idx_operation_logs_status ON operation_logs(status);
CREATE INDEX idx_operation_logs_created_at ON operation_logs(created_at DESC);
CREATE INDEX idx_operation_logs_business_type ON operation_logs(business_type);
CREATE INDEX idx_operation_logs_ip_address ON operation_logs(ip_address);

-- 登录日志表索引
CREATE INDEX idx_login_logs_user_id ON login_logs(user_id);
CREATE INDEX idx_login_logs_username ON login_logs(username);
CREATE INDEX idx_login_logs_status ON login_logs(status);
CREATE INDEX idx_login_logs_created_at ON login_logs(created_at DESC);
CREATE INDEX idx_login_logs_ip_address ON login_logs(ip_address);
CREATE INDEX idx_login_logs_login_type ON login_logs(login_type);

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
