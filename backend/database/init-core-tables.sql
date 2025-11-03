-- ============================================
-- 国创睿峰智能图书馆管理系统 - 核心表结构
-- Sprint 2 - MVP版本
-- 创建时间: 2025-10-12
-- ============================================

-- 设置时区
SET timezone = 'Asia/Shanghai';

-- ============================================
-- 1. 认证相关表 (auth_service)
-- ============================================

\c auth_service;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    role_id INT NOT NULL DEFAULT 3,
    status SMALLINT NOT NULL DEFAULT 1, -- 1:正常 0:禁用 2:锁定
    last_login_time TIMESTAMP,
    login_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    remark TEXT
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN users.real_name IS '真实姓名';
COMMENT ON COLUMN users.role_id IS '角色ID: 1-超级管理员 2-管理员 3-普通用户';
COMMENT ON COLUMN users.status IS '状态: 1-正常 0-禁用 2-锁定';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    role_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE roles IS '角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
    id SERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) UNIQUE NOT NULL,
    resource_type VARCHAR(50), -- menu, button, api
    resource_path VARCHAR(200),
    parent_id INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE permissions IS '权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, permission_id)
);

COMMENT ON TABLE role_permissions IS '角色权限关联表';

-- 用户登录日志
CREATE TABLE IF NOT EXISTS user_login_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    login_status SMALLINT, -- 1:成功 0:失败
    fail_reason VARCHAR(200)
);

COMMENT ON TABLE user_login_logs IS '用户登录日志';

-- ============================================
-- 2. 图书相关表 (book_service)
-- ============================================

\c book_service;

-- 图书分类表
CREATE TABLE IF NOT EXISTS book_categories (
    id SERIAL PRIMARY KEY,
    category_code VARCHAR(50) UNIQUE NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    parent_id INT DEFAULT 0,
    level INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE book_categories IS '图书分类表(中图分类法)';

-- 图书基本信息表
CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) UNIQUE,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(200),
    author VARCHAR(200),
    translator VARCHAR(200),
    publisher VARCHAR(100),
    publish_date DATE,
    edition VARCHAR(50),
    pages INT,
    price DECIMAL(10, 2),
    category_id INT,
    language VARCHAR(50) DEFAULT 'zh-CN',
    cover_url VARCHAR(500),
    description TEXT,
    keywords TEXT,
    total_quantity INT DEFAULT 0,
    available_quantity INT DEFAULT 0,
    borrowed_quantity INT DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1, -- 1:正常 0:下架 2:待审核
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

COMMENT ON TABLE books IS '图书基本信息表';
COMMENT ON COLUMN books.isbn IS 'ISBN号';
COMMENT ON COLUMN books.title IS '书名';
COMMENT ON COLUMN books.total_quantity IS '馆藏总数';
COMMENT ON COLUMN books.available_quantity IS '可借数量';
COMMENT ON COLUMN books.borrowed_quantity IS '已借数量';

-- 馆藏副本表 (每本实体书一条记录)
CREATE TABLE IF NOT EXISTS book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(50) UNIQUE NOT NULL,
    rfid_tag VARCHAR(50),
    location_code VARCHAR(50), -- 馆藏位置
    shelf_code VARCHAR(50), -- 书架号
    acquisition_date DATE, -- 采购日期
    acquisition_price DECIMAL(10, 2),
    binding_type VARCHAR(20), -- 装帧类型: 精装/平装
    copy_status SMALLINT NOT NULL DEFAULT 1, -- 1:在架 2:借出 3:预约 4:维修 5:丢失 6:注销
    condition_level SMALLINT DEFAULT 5, -- 品相等级 1-10
    borrow_count INT DEFAULT 0,
    last_borrow_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE book_copies IS '馆藏副本表';
COMMENT ON COLUMN book_copies.barcode IS '条形码';
COMMENT ON COLUMN book_copies.copy_status IS '副本状态: 1-在架 2-借出 3-预约 4-维修 5-丢失 6-注销';

-- ============================================
-- 3. 流通相关表 (circulation_service)
-- ============================================

\c circulation_service;

-- 借阅记录表
CREATE TABLE IF NOT EXISTS circulation_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    reader_name VARCHAR(100),
    reader_type VARCHAR(20), -- student/teacher/other
    book_id BIGINT NOT NULL,
    book_title VARCHAR(200),
    copy_id BIGINT NOT NULL,
    barcode VARCHAR(50),
    borrow_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NOT NULL,
    return_date TIMESTAMP,
    renew_count INT DEFAULT 0,
    overdue_days INT DEFAULT 0,
    fine_amount DECIMAL(10, 2) DEFAULT 0,
    fine_paid BOOLEAN DEFAULT FALSE,
    operator_id BIGINT,
    operator_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'borrowed', -- borrowed:借出 returned:已还 overdue:逾期
    return_condition SMALLINT, -- 归还时品相评级
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE circulation_records IS '借阅记录表';
COMMENT ON COLUMN circulation_records.status IS '状态: borrowed-借出 returned-已还 overdue-逾期';
COMMENT ON COLUMN circulation_records.overdue_days IS '逾期天数';
COMMENT ON COLUMN circulation_records.fine_amount IS '罚款金额';

-- 预约记录表
CREATE TABLE IF NOT EXISTS reservation_records (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    reader_name VARCHAR(100),
    book_id BIGINT NOT NULL,
    book_title VARCHAR(200),
    reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_date TIMESTAMP NOT NULL,
    notify_date TIMESTAMP,
    pickup_date TIMESTAMP,
    cancel_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending:等待中 available:可取书 picked:已取书 expired:已过期 cancelled:已取消
    priority INT DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE reservation_records IS '预约记录表';
COMMENT ON COLUMN reservation_records.status IS '状态: pending-等待 available-可取 picked-已取 expired-过期 cancelled-取消';

-- 续借记录表
CREATE TABLE IF NOT EXISTS renewal_records (
    id BIGSERIAL PRIMARY KEY,
    circulation_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    old_due_date TIMESTAMP NOT NULL,
    new_due_date TIMESTAMP NOT NULL,
    renewal_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operator_id BIGINT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE renewal_records IS '续借记录表';

-- ============================================
-- 创建索引
-- ============================================

\c auth_service;
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_user_login_logs_user_id ON user_login_logs(user_id);
CREATE INDEX idx_user_login_logs_login_time ON user_login_logs(login_time);

\c book_service;
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_category_id ON books(category_id);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_book_copies_book_id ON book_copies(book_id);
CREATE INDEX idx_book_copies_barcode ON book_copies(barcode);
CREATE INDEX idx_book_copies_status ON book_copies(copy_status);

\c circulation_service;
CREATE INDEX idx_circulation_reader_id ON circulation_records(reader_id);
CREATE INDEX idx_circulation_book_id ON circulation_records(book_id);
CREATE INDEX idx_circulation_copy_id ON circulation_records(copy_id);
CREATE INDEX idx_circulation_status ON circulation_records(status);
CREATE INDEX idx_circulation_borrow_date ON circulation_records(borrow_date);
CREATE INDEX idx_circulation_due_date ON circulation_records(due_date);
CREATE INDEX idx_reservation_reader_id ON reservation_records(reader_id);
CREATE INDEX idx_reservation_book_id ON reservation_records(book_id);
CREATE INDEX idx_reservation_status ON reservation_records(status);

-- ============================================
-- 插入初始数据
-- ============================================

\c auth_service;

-- 插入默认角色
INSERT INTO roles (id, role_name, role_code, description) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '系统最高权限'),
(2, '管理员', 'ADMIN', '图书馆管理员'),
(3, '普通用户', 'USER', '读者用户')
ON CONFLICT (id) DO NOTHING;

-- 插入默认管理员账号 (密码: admin123, BCrypt加密后的hash)
INSERT INTO users (username, password, real_name, email, role_id, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbtPJjBr7xGR6X.LE', '系统管理员', 'admin@gcrf.com', 1, 1)
ON CONFLICT (username) DO NOTHING;

\c book_service;

-- 插入图书分类(中图分类法一级分类)
INSERT INTO book_categories (category_code, category_name, parent_id, level, sort_order) VALUES
('A', '马克思主义、列宁主义、毛泽东思想、邓小平理论', 0, 1, 1),
('B', '哲学、宗教', 0, 1, 2),
('C', '社会科学总论', 0, 1, 3),
('D', '政治、法律', 0, 1, 4),
('E', '军事', 0, 1, 5),
('F', '经济', 0, 1, 6),
('G', '文化、科学、教育、体育', 0, 1, 7),
('H', '语言、文字', 0, 1, 8),
('I', '文学', 0, 1, 9),
('J', '艺术', 0, 1, 10),
('K', '历史、地理', 0, 1, 11),
('N', '自然科学总论', 0, 1, 12),
('O', '数理科学和化学', 0, 1, 13),
('P', '天文学、地球科学', 0, 1, 14),
('Q', '生物科学', 0, 1, 15),
('R', '医药、卫生', 0, 1, 16),
('S', '农业科学', 0, 1, 17),
('T', '工业技术', 0, 1, 18),
('U', '交通运输', 0, 1, 19),
('V', '航空、航天', 0, 1, 20),
('X', '环境科学、安全科学', 0, 1, 21),
('Z', '综合性图书', 0, 1, 22)
ON CONFLICT (category_code) DO NOTHING;

-- ============================================
-- 授予权限
-- ============================================

\c auth_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

\c book_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

\c circulation_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- 完成
\echo '✅ 核心表结构创建完成！'
\echo '✅ 默认管理员账号: admin / admin123'
\echo '✅ 数据库: auth_service, book_service, circulation_service'
