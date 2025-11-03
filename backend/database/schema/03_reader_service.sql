-- =========================================
-- 读者服务数据库表结构
-- Database: reader_service
-- Description: 读者管理相关表
-- Version: 1.0.0
-- Date: 2025-10-28
-- =========================================

-- 注意: 此脚本会删除并重建reader_service数据库
-- ⚠️ 警告: 执行此脚本将删除所有现有数据！请确保已备份重要数据
-- 如果只需要更新表结构，请注释掉DROP DATABASE语句

-- 删除并重建数据库
DROP DATABASE IF EXISTS reader_service;
CREATE DATABASE reader_service WITH ENCODING 'UTF8';

-- 连接到reader_service数据库
\c reader_service;

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- 用于模糊搜索
CREATE EXTENSION IF NOT EXISTS "zhparser"; -- 中文分词(可选)

-- =========================================
-- 1. 读者表 (readers)
-- Total fields: 23 (excluding system fields like id)
-- =========================================
CREATE TABLE IF NOT EXISTS readers (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 业务标识
    reader_id VARCHAR(20) UNIQUE NOT NULL,                -- 读者证号(格式: YYYY0001)

    -- 基本信息
    name VARCHAR(100) NOT NULL,                           -- 姓名
    id_card VARCHAR(18) UNIQUE,                          -- 身份证号
    phone VARCHAR(20) UNIQUE,                            -- 手机号
    email VARCHAR(100) UNIQUE,                           -- 邮箱

    -- 读者类型信息
    reader_type VARCHAR(20) NOT NULL,                     -- 读者类型: STUDENT/TEACHER/STAFF/EXTERNAL
    department VARCHAR(100),                              -- 院系/部门
    student_no VARCHAR(50),                               -- 学号(学生专用)
    employee_no VARCHAR(50),                              -- 工号(教师/职工专用)

    -- 借阅权限
    max_borrow_count INTEGER NOT NULL DEFAULT 5,          -- 最大借阅数量
    max_borrow_days INTEGER NOT NULL DEFAULT 30,         -- 最长借阅天数

    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',        -- 状态: ACTIVE-正常, SUSPENDED-停用, EXPIRED-过期
    expiry_date DATE,                                    -- 证件有效期

    -- 其他信息
    avatar_url VARCHAR(500),                             -- 头像URL

    -- 时间戳字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted_at TIMESTAMP,                                     -- 删除时间(软删除)

    -- 约束条件
    CONSTRAINT chk_reader_type CHECK (reader_type IN ('STUDENT', 'TEACHER', 'STAFF', 'EXTERNAL')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT chk_max_borrow_count CHECK (max_borrow_count >= 0),
    CONSTRAINT chk_max_borrow_days CHECK (max_borrow_days > 0)
);

-- =========================================
-- 2. 创建索引
-- =========================================

-- 唯一索引
-- reader_id, id_card, phone, email 已经在表定义中设置为UNIQUE

-- 普通索引 - 高频查询字段
CREATE INDEX idx_readers_name ON readers(name);
CREATE INDEX idx_readers_reader_type ON readers(reader_type);
CREATE INDEX idx_readers_status ON readers(status);
CREATE INDEX idx_readers_department ON readers(department);
CREATE INDEX idx_readers_student_no ON readers(student_no) WHERE student_no IS NOT NULL;
CREATE INDEX idx_readers_employee_no ON readers(employee_no) WHERE employee_no IS NOT NULL;
CREATE INDEX idx_readers_expiry_date ON readers(expiry_date);

-- 复合索引 - 常见组合查询
CREATE INDEX idx_readers_type_status ON readers(reader_type, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_readers_status_expiry ON readers(status, expiry_date) WHERE deleted_at IS NULL;

-- 部分索引 - 优化活跃读者查询
CREATE INDEX idx_readers_active ON readers(reader_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_readers_suspended ON readers(reader_id) WHERE status = 'SUSPENDED' AND deleted_at IS NULL;

-- 全文搜索索引 (PostgreSQL GIN索引)
CREATE INDEX idx_readers_name_gin ON readers USING gin(to_tsvector('simple', name));

-- 模糊搜索索引 (使用pg_trgm)
CREATE INDEX idx_readers_name_trgm ON readers USING gin(name gin_trgm_ops);
CREATE INDEX idx_readers_phone_trgm ON readers USING gin(phone gin_trgm_ops) WHERE phone IS NOT NULL;
CREATE INDEX idx_readers_email_trgm ON readers USING gin(email gin_trgm_ops) WHERE email IS NOT NULL;

-- 软删除优化索引
CREATE INDEX idx_readers_deleted_at ON readers(deleted_at) WHERE deleted_at IS NULL;

-- =========================================
-- 3. 创建触发器 - 自动更新updated_at
-- =========================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_readers_updated_at
    BEFORE UPDATE ON readers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- 4. 插入测试数据
-- =========================================
INSERT INTO readers (
    reader_id, name, id_card, phone, email,
    reader_type, department, student_no, employee_no,
    max_borrow_count, max_borrow_days, status, expiry_date, avatar_url
) VALUES
-- 学生读者 (20个)
('20240001', '张晓明', '110105200301151234', '13811111111', 'zhangxm@student.edu.cn',
    'STUDENT', '计算机科学与技术学院', '2021001', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/001.jpg'),

('20240002', '李雨晨', '110108200302251567', '13822222222', 'liyc@student.edu.cn',
    'STUDENT', '电子信息工程学院', '2021002', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/002.jpg'),

('20240003', '王思远', '110106200301083456', '13833333333', 'wangsy@student.edu.cn',
    'STUDENT', '机械工程学院', '2021003', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/003.jpg'),

('20240004', '陈佳琪', '110109200302174589', '13844444444', 'chenjq@student.edu.cn',
    'STUDENT', '经济管理学院', '2021004', NULL,
    10, 30, 'SUSPENDED', '2025-06-30', 'https://avatars.example.com/student/004.jpg'),

('20240005', '刘梦婷', '110107200301297890', '13855555555', 'liumt@student.edu.cn',
    'STUDENT', '文学与传媒学院', '2021005', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/005.jpg'),

('20240006', '赵文博', '110102200302051234', '13866666666', 'zhaowb@student.edu.cn',
    'STUDENT', '数学与统计学院', '2021006', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/006.jpg'),

('20240007', '周晓雪', '110101200301164567', '13877777777', 'zhouxx@student.edu.cn',
    'STUDENT', '外国语学院', '2021007', NULL,
    10, 30, 'EXPIRED', '2024-06-30', 'https://avatars.example.com/student/007.jpg'),

('20240008', '吴浩然', '110104200302238901', '13888888888', 'wuhr@student.edu.cn',
    'STUDENT', '物理与光电工程学院', '2021008', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/008.jpg'),

('20240009', '郑雅琴', '110103200301092345', '13899999999', 'zhengyq@student.edu.cn',
    'STUDENT', '化学与材料科学学院', '2021009', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/009.jpg'),

('20240010', '孙志强', '110110200302186789', '13900000000', 'sunzq@student.edu.cn',
    'STUDENT', '生命科学学院', '2021010', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/010.jpg'),

('20240011', '马晓宇', '110111200301270123', '13911111111', 'maxy@student.edu.cn',
    'STUDENT', '建筑与城市规划学院', '2021011', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/011.jpg'),

('20240012', '徐梦洁', '110112200302034567', '13922222222', 'xumj@student.edu.cn',
    'STUDENT', '艺术设计学院', '2021012', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/012.jpg'),

('20240013', '冯博文', '110105200301148901', '13933333333', 'fengbw@student.edu.cn',
    'STUDENT', '法学院', '2021013', NULL,
    10, 30, 'SUSPENDED', '2025-06-30', 'https://avatars.example.com/student/013.jpg'),

('20240014', '杨雪莉', '110108200302212345', '13944444444', 'yangxl@student.edu.cn',
    'STUDENT', '教育学院', '2021014', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/014.jpg'),

('20240015', '黄俊杰', '110106200301056789', '13955555555', 'huangjj@student.edu.cn',
    'STUDENT', '体育学院', '2021015', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/015.jpg'),

('20240016', '林诗涵', '110109200302190123', '13966666666', 'linsh@student.edu.cn',
    'STUDENT', '音乐学院', '2021016', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/016.jpg'),

('20240017', '谢明轩', '110107200301234567', '13977777777', 'xiemx@student.edu.cn',
    'STUDENT', '医学院', '2021017', NULL,
    10, 30, 'EXPIRED', '2024-06-30', 'https://avatars.example.com/student/017.jpg'),

('20240018', '何雨欣', '110102200302078901', '13988888888', 'heyx@student.edu.cn',
    'STUDENT', '药学院', '2021018', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/018.jpg'),

('20240019', '罗天宇', '110101200301112345', '13999999999', 'luoty@student.edu.cn',
    'STUDENT', '公共管理学院', '2021019', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/019.jpg'),

('20240020', '韩雅静', '110104200302156789', '15000000000', 'hanyj@student.edu.cn',
    'STUDENT', '马克思主义学院', '2021020', NULL,
    10, 30, 'ACTIVE', '2025-06-30', 'https://avatars.example.com/student/020.jpg'),

-- 教师读者 (10个)
('20240021', '陈建国', '110105198001150234', '15011111111', 'chenjg@teacher.edu.cn',
    'TEACHER', '计算机科学与技术学院', NULL, 'T001',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/001.jpg'),

('20240022', '王秀英', '110108197905251567', '15022222222', 'wangxy@teacher.edu.cn',
    'TEACHER', '电子信息工程学院', NULL, 'T002',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/002.jpg'),

('20240023', '李文斌', '110106198203083456', '15033333333', 'liwb@teacher.edu.cn',
    'TEACHER', '机械工程学院', NULL, 'T003',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/003.jpg'),

('20240024', '张丽华', '110109197802174589', '15044444444', 'zhanglh@teacher.edu.cn',
    'TEACHER', '经济管理学院', NULL, 'T004',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/004.jpg'),

('20240025', '刘志强', '110107198101297890', '15055555555', 'liuzq@teacher.edu.cn',
    'TEACHER', '文学与传媒学院', NULL, 'T005',
    20, 60, 'SUSPENDED', '2026-12-31', 'https://avatars.example.com/teacher/005.jpg'),

('20240026', '赵明阳', '110102197702051234', '15066666666', 'zhaomy@teacher.edu.cn',
    'TEACHER', '数学与统计学院', NULL, 'T006',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/006.jpg'),

('20240027', '周华民', '110101198301164567', '15077777777', 'zhouhm@teacher.edu.cn',
    'TEACHER', '外国语学院', NULL, 'T007',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/007.jpg'),

('20240028', '吴晓峰', '110104197902238901', '15088888888', 'wuxf@teacher.edu.cn',
    'TEACHER', '物理与光电工程学院', NULL, 'T008',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/008.jpg'),

('20240029', '郑文杰', '110103198201092345', '15099999999', 'zhengwj@teacher.edu.cn',
    'TEACHER', '化学与材料科学学院', NULL, 'T009',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/009.jpg'),

('20240030', '孙美玲', '110110197802186789', '15100000000', 'sunml@teacher.edu.cn',
    'TEACHER', '生命科学学院', NULL, 'T010',
    20, 60, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/teacher/010.jpg'),

-- 职工读者 (5个)
('20240031', '马建军', '110111198501270123', '15111111111', 'majj@staff.edu.cn',
    'STAFF', '图书馆', NULL, 'S001',
    15, 45, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/staff/001.jpg'),

('20240032', '徐晓红', '110112198602034567', '15122222222', 'xuxh@staff.edu.cn',
    'STAFF', '教务处', NULL, 'S002',
    15, 45, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/staff/002.jpg'),

('20240033', '冯国庆', '110105198701148901', '15133333333', 'fenggq@staff.edu.cn',
    'STAFF', '财务处', NULL, 'S003',
    15, 45, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/staff/003.jpg'),

('20240034', '杨丽萍', '110108198802212345', '15144444444', 'yanglp@staff.edu.cn',
    'STAFF', '后勤处', NULL, 'S004',
    15, 45, 'EXPIRED', '2024-12-31', 'https://avatars.example.com/staff/004.jpg'),

('20240035', '黄志伟', '110106198901056789', '15155555555', 'huangzw@staff.edu.cn',
    'STAFF', '保卫处', NULL, 'S005',
    15, 45, 'ACTIVE', '2026-12-31', 'https://avatars.example.com/staff/005.jpg'),

-- 外部读者 (5个)
('20240036', '林海涛', '110109199002190123', '15166666666', 'linht@external.com',
    'EXTERNAL', NULL, NULL, NULL,
    3, 15, 'ACTIVE', '2025-03-31', 'https://avatars.example.com/external/001.jpg'),

('20240037', '谢敏敏', '110107199101234567', '15177777777', 'xiemm@external.com',
    'EXTERNAL', NULL, NULL, NULL,
    3, 15, 'ACTIVE', '2025-03-31', 'https://avatars.example.com/external/002.jpg'),

('20240038', '何建华', '110102199202078901', '15188888888', 'hejh@external.com',
    'EXTERNAL', NULL, NULL, NULL,
    3, 15, 'SUSPENDED', '2025-03-31', 'https://avatars.example.com/external/003.jpg'),

('20240039', '罗晓芳', '110101199301112345', '15199999999', 'luoxf@external.com',
    'EXTERNAL', NULL, NULL, NULL,
    3, 15, 'ACTIVE', '2025-03-31', 'https://avatars.example.com/external/004.jpg'),

('20240040', '韩东明', '110104199402156789', '15200000000', 'handm@external.com',
    'EXTERNAL', NULL, NULL, NULL,
    3, 15, 'ACTIVE', '2025-03-31', 'https://avatars.example.com/external/005.jpg');

-- =========================================
-- 5. 添加表注释
-- =========================================
COMMENT ON TABLE readers IS '读者信息表';
COMMENT ON COLUMN readers.id IS '读者ID(主键)';
COMMENT ON COLUMN readers.reader_id IS '读者证号(业务标识)';
COMMENT ON COLUMN readers.name IS '姓名';
COMMENT ON COLUMN readers.id_card IS '身份证号';
COMMENT ON COLUMN readers.phone IS '手机号码';
COMMENT ON COLUMN readers.email IS '电子邮箱';
COMMENT ON COLUMN readers.reader_type IS '读者类型(STUDENT-学生/TEACHER-教师/STAFF-职工/EXTERNAL-外部)';
COMMENT ON COLUMN readers.department IS '所属院系或部门';
COMMENT ON COLUMN readers.student_no IS '学号(学生专用)';
COMMENT ON COLUMN readers.employee_no IS '工号(教师/职工专用)';
COMMENT ON COLUMN readers.max_borrow_count IS '最大可借阅数量';
COMMENT ON COLUMN readers.max_borrow_days IS '最长借阅天数';
COMMENT ON COLUMN readers.status IS '状态(ACTIVE-正常/SUSPENDED-停用/EXPIRED-过期)';
COMMENT ON COLUMN readers.expiry_date IS '证件有效期';
COMMENT ON COLUMN readers.avatar_url IS '头像URL';
COMMENT ON COLUMN readers.created_at IS '创建时间';
COMMENT ON COLUMN readers.updated_at IS '更新时间';
COMMENT ON COLUMN readers.deleted_at IS '删除时间(软删除)';

-- =========================================
-- 6. 数据统计
-- =========================================
-- 查看插入的数据统计
SELECT
    COUNT(*) as total_readers,
    COUNT(DISTINCT reader_type) as reader_types,
    COUNT(CASE WHEN reader_type = 'STUDENT' THEN 1 END) as students,
    COUNT(CASE WHEN reader_type = 'TEACHER' THEN 1 END) as teachers,
    COUNT(CASE WHEN reader_type = 'STAFF' THEN 1 END) as staff,
    COUNT(CASE WHEN reader_type = 'EXTERNAL' THEN 1 END) as external,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_readers,
    COUNT(CASE WHEN status = 'SUSPENDED' THEN 1 END) as suspended_readers,
    COUNT(CASE WHEN status = 'EXPIRED' THEN 1 END) as expired_readers
FROM readers;

-- 按读者类型和状态统计
SELECT
    reader_type,
    status,
    COUNT(*) as count,
    AVG(max_borrow_count) as avg_max_borrow,
    AVG(max_borrow_days) as avg_max_days
FROM readers
GROUP BY reader_type, status
ORDER BY reader_type, status;

-- =========================================
-- 脚本执行完成
-- Total Fields in readers table: 14 (excluding id and timestamps)
-- Total Indexes Created: 17
-- Total Sample Readers Inserted: 40
--   - Students: 20
--   - Teachers: 10
--   - Staff: 5
--   - External: 5
-- =========================================