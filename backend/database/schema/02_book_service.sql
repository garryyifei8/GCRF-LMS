-- =========================================
-- 图书服务数据库表结构
-- Database: book_service
-- Description: 图书管理相关表
-- Version: 1.0.0
-- Date: 2025-10-28
-- =========================================

-- 注意: 请在执行此脚本前,先手动创建book_service数据库
-- CREATE DATABASE book_service WITH ENCODING 'UTF8';
-- 连接到book_service数据库后执行以下脚本

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- 用于模糊搜索
CREATE EXTENSION IF NOT EXISTS "zhparser"; -- 中文分词(可选)

-- =========================================
-- 1. 图书表 (books)
-- Total fields: 24 (excluding system fields like id)
-- =========================================
CREATE TABLE IF NOT EXISTS books (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 基本信息
    isbn VARCHAR(20) UNIQUE NOT NULL,                     -- ISBN号(国际标准书号)
    title VARCHAR(500) NOT NULL,                          -- 图书标题
    subtitle VARCHAR(500),                                -- 副标题
    author VARCHAR(500) NOT NULL,                         -- 作者(可能多个,用逗号分隔)
    translator VARCHAR(500),                              -- 译者(可能多个,用逗号分隔)
    publisher VARCHAR(200),                               -- 出版社
    publish_date DATE,                                    -- 出版日期
    edition VARCHAR(50),                                  -- 版次(如:第1版)

    -- 物理属性
    pages INTEGER CHECK (pages > 0),                      -- 页数
    price DECIMAL(10, 2) CHECK (price >= 0),             -- 价格
    binding VARCHAR(50),                                  -- 装帧(精装/平装/线装等)
    language VARCHAR(50) DEFAULT '中文',                   -- 语言

    -- 分类信息
    classification_code VARCHAR(50),                      -- 分类代码(如:TP312-Java编程)
    subject_keywords VARCHAR(500),                        -- 主题关键词(用逗号分隔)
    abstract TEXT,                                        -- 摘要/简介(使用abstract作为列名)

    -- 媒体信息
    cover_url VARCHAR(500),                              -- 封面图片URL

    -- 库存信息
    total_quantity INTEGER DEFAULT 1 CHECK (total_quantity >= 0),      -- 馆藏总数
    available_quantity INTEGER DEFAULT 1 CHECK (available_quantity >= 0), -- 可借数量

    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',        -- 状态: ACTIVE-正常, INACTIVE-下架

    -- 时间戳字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
    deleted_at TIMESTAMP,                                     -- 删除时间(软删除)

    -- 约束条件
    CONSTRAINT chk_available_quantity CHECK (available_quantity <= total_quantity),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- =========================================
-- 2. 创建索引
-- =========================================

-- 唯一索引
-- ISBN已经在表定义中设置为UNIQUE

-- 普通索引 - 高频查询字段
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_publisher ON books(publisher);
CREATE INDEX idx_books_status ON books(status);
CREATE INDEX idx_books_classification_code ON books(classification_code);
CREATE INDEX idx_books_publish_date ON books(publish_date);

-- 复合索引 - 常见组合查询
CREATE INDEX idx_books_status_available ON books(status, available_quantity) WHERE deleted_at IS NULL;
CREATE INDEX idx_books_author_title ON books(author, title);

-- 全文搜索索引 (PostgreSQL GIN索引)
CREATE INDEX idx_books_title_gin ON books USING gin(to_tsvector('simple', title));
CREATE INDEX idx_books_author_gin ON books USING gin(to_tsvector('simple', author));
CREATE INDEX idx_books_keywords_gin ON books USING gin(to_tsvector('simple', coalesce(subject_keywords, '')));

-- 模糊搜索索引 (使用pg_trgm)
CREATE INDEX idx_books_title_trgm ON books USING gin(title gin_trgm_ops);
CREATE INDEX idx_books_author_trgm ON books USING gin(author gin_trgm_ops);

-- 软删除优化索引
CREATE INDEX idx_books_deleted_at ON books(deleted_at) WHERE deleted_at IS NULL;

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

CREATE TRIGGER update_books_updated_at
    BEFORE UPDATE ON books
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- 4. 插入测试数据
-- =========================================
INSERT INTO books (
    isbn, title, subtitle, author, translator, publisher,
    publish_date, edition, pages, price, binding, language,
    classification_code, subject_keywords, abstract, cover_url,
    total_quantity, available_quantity, status
) VALUES
-- 技术类图书
('9787115544063', 'Java核心技术 卷I', '基础知识(原书第11版)', 'Cay S. Horstmann', '林琪,苏钰函', '人民邮电出版社',
    '2020-09-01', '第11版', 742, 149.00, '平装', '中文',
    'TP312JA', 'Java,编程,面向对象,多线程,集合框架',
    '本书是Java技术经典参考书,全面覆盖Java SE 11的新特性,深入浅出地介绍了Java语言与核心类库。',
    'https://img3.doubanio.com/view/subject/l/public/s33665863.jpg',
    5, 3, 'ACTIVE'),

('9787121411748', 'Spring Boot实战', '微服务开发、测试与部署', 'Craig Walls', '丁雪丰', '电子工业出版社',
    '2021-07-01', '第1版', 392, 89.00, '平装', '中文',
    'TP312SP', 'Spring Boot,微服务,Java,REST API,云原生',
    'Spring Boot让Spring应用开发变得简单、高效。本书全面介绍Spring Boot开发的方方面面。',
    'https://img9.doubanio.com/view/subject/l/public/s33821274.jpg',
    3, 2, 'ACTIVE'),

('9787302570646', '深入理解Java虚拟机', 'JVM高级特性与最佳实践(第3版)', '周志明', NULL, '清华大学出版社',
    '2021-01-01', '第3版', 540, 129.00, '平装', '中文',
    'TP312JA', 'JVM,Java虚拟机,性能优化,垃圾回收,内存模型',
    '围绕内存管理、执行子系统、程序编译与优化、高效并发等核心主题对JVM进行了全面而深入的分析。',
    'https://img1.doubanio.com/view/subject/l/public/s33585910.jpg',
    4, 2, 'ACTIVE'),

-- 文学类图书
('9787020002207', '红楼梦', NULL, '曹雪芹,高鹗', NULL, '人民文学出版社',
    '2008-07-01', '第3版', 1606, 59.70, '平装', '中文',
    'I242.4', '古典文学,四大名著,清代小说,爱情故事',
    '中国古典小说的巅峰之作,以贾、史、王、薛四大家族的兴衰为背景,展现了18世纪上半叶中国社会的方方面面。',
    'https://img3.doubanio.com/view/subject/l/public/s3253563.jpg',
    10, 8, 'ACTIVE'),

('9787544277617', '活着', NULL, '余华', NULL, '南海出版公司',
    '2017-06-01', '第1版', 191, 28.00, '平装', '中文',
    'I247.5', '当代文学,余华,生命,苦难,人性',
    '讲述了在内战、大跃进等社会变革下,主人公福贵的人生和家庭不断经受苦难的故事。',
    'https://img9.doubanio.com/view/subject/l/public/s29651114.jpg',
    8, 6, 'ACTIVE'),

('9787544291163', '百年孤独', NULL, '加西亚·马尔克斯', '范晔', '南海出版公司',
    '2017-08-01', '第1版', 360, 55.00, '精装', '中文',
    'I775.45', '魔幻现实主义,拉美文学,诺贝尔文学奖,家族史诗',
    '魔幻现实主义文学的代表作,描写了布恩迪亚家族七代人的传奇故事。',
    'https://img1.doubanio.com/view/subject/l/public/s29632659.jpg',
    6, 4, 'ACTIVE'),

-- 历史类图书
('9787101115970', '史记', NULL, '司马迁', '司马迁著,裴骃集解,司马贞索隐,张守节正义', '中华书局',
    '2014-08-01', '第1版', 3338, 125.00, '精装', '中文',
    'K204.2', '历史,纪传体,中国古代史,二十四史',
    '中国历史上第一部纪传体通史,记载了从上古传说中的黄帝时代到汉武帝太初四年间共3000多年的历史。',
    'https://img3.doubanio.com/view/subject/l/public/s28282063.jpg',
    4, 3, 'ACTIVE'),

('9787508699134', '万历十五年', NULL, '黄仁宇', NULL, '中华书局',
    '2019-05-01', '增订纪念版', 280, 52.00, '精装', '中文',
    'K248.3', '明史,历史,黄仁宇,万历,大历史观',
    '从大历史观的角度,以1587年为支点,剖析明朝社会的各个层面及其症结。',
    'https://img9.doubanio.com/view/subject/l/public/s33521204.jpg',
    5, 4, 'ACTIVE'),

-- 科普类图书
('9787535794604', '时间简史', '从大爆炸到黑洞', '史蒂芬·霍金', '许明贤,吴忠超', '湖南科学技术出版社',
    '2018-04-01', '插图版', 247, 45.00, '平装', '中文',
    'P159', '物理学,宇宙学,科普,黑洞,时空',
    '探索时间和空间的核心秘密,解释宇宙的起源和归宿,是当代最重要的科普经典。',
    'https://img3.doubanio.com/view/subject/l/public/s29440153.jpg',
    3, 2, 'ACTIVE'),

-- 经济管理类图书
('9787521718140', '原则', '生活和工作', '瑞·达利欧', '刘波,綦相', '中信出版社',
    '2020-01-01', '第1版', 576, 98.00, '精装', '中文',
    'F272', '管理学,投资,人生哲学,决策,桥水基金',
    '桥水基金创始人达利欧分享的生活和工作原则,帮助读者认识自己、实现目标。',
    'https://img9.doubanio.com/view/subject/l/public/s33469624.jpg',
    4, 3, 'ACTIVE'),

-- 哲学类图书
('9787532776771', '苏菲的世界', '关于哲学史的小说', '乔斯坦·贾德', '萧宝森', '上海译文出版社',
    '2017-08-01', '第1版', 536, 58.00, '精装', '中文',
    'B0', '哲学,哲学史,西方哲学,入门,小说',
    '以小说的形式,通过一名哲学导师向一个叫苏菲的女孩传授哲学知识的经过,揭示了西方哲学史发展的历程。',
    'https://img1.doubanio.com/view/subject/l/public/s29488088.jpg',
    6, 5, 'ACTIVE'),

-- 心理学类图书
('9787300191966', '心流', '最优体验心理学', '米哈里·契克森米哈赖', '张定绮', '中国人民大学出版社',
    '2017-11-01', '第1版', 304, 49.90, '平装', '中文',
    'B84', '心理学,积极心理学,幸福,专注力,自我实现',
    '心流是指全神贯注于某项活动而体验到的一种整体感受。本书系统阐述了心流理论。',
    'https://img3.doubanio.com/view/subject/l/public/s29420863.jpg',
    3, 2, 'ACTIVE'),

-- 艺术类图书
('9787108058935', '美的历程', NULL, '李泽厚', NULL, '生活·读书·新知三联书店',
    '2017-06-01', '第1版', 321, 58.00, '平装', '中文',
    'J120.9', '美学,艺术史,中国文化,哲学,李泽厚',
    '从宏观角度鸟瞰中国数千年的艺术、文学发展,提出了诸多创见性的美学观点。',
    'https://img9.doubanio.com/view/subject/l/public/s29433064.jpg',
    4, 3, 'ACTIVE'),

-- 教育类图书
('9787508672069', '如何阅读一本书', NULL, '莫提默·J·艾德勒,查尔斯·范多伦', '郝明义,朱衣', '中信出版社',
    '2016-10-01', '第1版', 424, 56.00, '平装', '中文',
    'G792', '阅读方法,学习方法,思维训练,教育',
    '一本指导人们如何阅读的经典指南,介绍了阅读的四个层次,帮助读者提升阅读技巧。',
    'https://img3.doubanio.com/view/subject/l/public/s29002503.jpg',
    5, 4, 'ACTIVE'),

-- 儿童文学
('9787020137275', '小王子', NULL, '安托万·德·圣-埃克苏佩里', '李继宏', '人民文学出版社',
    '2018-05-01', '第1版', 152, 32.00, '精装', '中文',
    'I565.45', '童话,法国文学,哲理,成长,经典',
    '讲述了小王子从自己星球出发前往地球的过程中,所经历的各种历险。',
    'https://img3.doubanio.com/view/subject/l/public/s33312543.jpg',
    8, 7, 'ACTIVE');

-- =========================================
-- 5. 添加表注释
-- =========================================
COMMENT ON TABLE books IS '图书信息表';
COMMENT ON COLUMN books.id IS '图书ID(主键)';
COMMENT ON COLUMN books.isbn IS 'ISBN号(国际标准书号)';
COMMENT ON COLUMN books.title IS '图书标题';
COMMENT ON COLUMN books.subtitle IS '副标题';
COMMENT ON COLUMN books.author IS '作者(多个用逗号分隔)';
COMMENT ON COLUMN books.translator IS '译者(多个用逗号分隔)';
COMMENT ON COLUMN books.publisher IS '出版社';
COMMENT ON COLUMN books.publish_date IS '出版日期';
COMMENT ON COLUMN books.edition IS '版次';
COMMENT ON COLUMN books.pages IS '页数';
COMMENT ON COLUMN books.price IS '定价';
COMMENT ON COLUMN books.binding IS '装帧方式(精装/平装等)';
COMMENT ON COLUMN books.language IS '语言';
COMMENT ON COLUMN books.classification_code IS '中图分类号';
COMMENT ON COLUMN books.subject_keywords IS '主题关键词(用逗号分隔)';
COMMENT ON COLUMN books.abstract IS '内容摘要/简介';
COMMENT ON COLUMN books.cover_url IS '封面图片URL';
COMMENT ON COLUMN books.total_quantity IS '馆藏总数';
COMMENT ON COLUMN books.available_quantity IS '可借数量';
COMMENT ON COLUMN books.status IS '状态(ACTIVE-正常/INACTIVE-下架)';
COMMENT ON COLUMN books.created_at IS '创建时间';
COMMENT ON COLUMN books.updated_at IS '更新时间';
COMMENT ON COLUMN books.deleted_at IS '删除时间(软删除)';

-- =========================================
-- 6. 数据统计
-- =========================================
-- 查看插入的数据统计
SELECT
    COUNT(*) as total_books,
    COUNT(DISTINCT publisher) as publishers,
    COUNT(DISTINCT classification_code) as categories,
    SUM(total_quantity) as total_inventory,
    SUM(available_quantity) as total_available,
    ROUND(AVG(price), 2) as avg_price
FROM books;

-- 按分类统计
SELECT
    SUBSTRING(classification_code, 1, 1) as category,
    COUNT(*) as book_count,
    SUM(total_quantity) as total_copies
FROM books
GROUP BY SUBSTRING(classification_code, 1, 1)
ORDER BY category;

-- =========================================
-- 脚本执行完成
-- Total Fields in books table: 24 (excluding id)
-- Total Indexes Created: 13
-- Total Sample Books Inserted: 15
-- =========================================