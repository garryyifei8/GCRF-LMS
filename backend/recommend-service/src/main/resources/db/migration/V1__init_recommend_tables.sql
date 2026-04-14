-- =====================================================
-- GCRF Library Management System - Recommend Service
-- Database Migration Script V1
-- Create recommendation related tables
-- =====================================================

-- 借阅历史表（从流通服务同步或视图映射）
CREATE TABLE IF NOT EXISTS borrow_history (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    book_title VARCHAR(500),
    category_code VARCHAR(50),
    borrow_time TIMESTAMP NOT NULL,
    return_time TIMESTAMP,
    borrow_days INTEGER,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    implicit_rating DECIMAL(3,2) DEFAULT 3.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_borrow_history_reader ON borrow_history(reader_id);
CREATE INDEX idx_borrow_history_book ON borrow_history(book_id);
CREATE INDEX idx_borrow_history_borrow_time ON borrow_history(borrow_time);
CREATE INDEX idx_borrow_history_category ON borrow_history(category_code);

-- 用户相似度矩阵表
CREATE TABLE IF NOT EXISTS user_similarity (
    id BIGSERIAL PRIMARY KEY,
    user_id_a BIGINT NOT NULL,
    user_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(10,8) NOT NULL,
    common_items_count INTEGER DEFAULT 0,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_similarity UNIQUE (user_id_a, user_id_b)
);

-- 索引
CREATE INDEX idx_user_similarity_a ON user_similarity(user_id_a);
CREATE INDEX idx_user_similarity_b ON user_similarity(user_id_b);
CREATE INDEX idx_user_similarity_score ON user_similarity(similarity_score DESC);

-- 物品（图书）相似度矩阵表
CREATE TABLE IF NOT EXISTS item_similarity (
    id BIGSERIAL PRIMARY KEY,
    book_id_a BIGINT NOT NULL,
    book_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(10,8) NOT NULL,
    common_users_count INTEGER DEFAULT 0,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_item_similarity UNIQUE (book_id_a, book_id_b)
);

-- 索引
CREATE INDEX idx_item_similarity_a ON item_similarity(book_id_a);
CREATE INDEX idx_item_similarity_b ON item_similarity(book_id_b);
CREATE INDEX idx_item_similarity_score ON item_similarity(similarity_score DESC);

-- 推荐日志表（用于评估推荐效果）
CREATE TABLE IF NOT EXISTS recommendation_log (
    id BIGSERIAL PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    score DECIMAL(10,8),
    algorithm VARCHAR(50) NOT NULL,
    scene VARCHAR(50),
    reason TEXT,
    clicked BOOLEAN DEFAULT FALSE,
    clicked_at TIMESTAMP,
    borrowed BOOLEAN DEFAULT FALSE,
    borrowed_at TIMESTAMP,
    recommended_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_recommendation_log_reader ON recommendation_log(reader_id);
CREATE INDEX idx_recommendation_log_book ON recommendation_log(book_id);
CREATE INDEX idx_recommendation_log_algorithm ON recommendation_log(algorithm);
CREATE INDEX idx_recommendation_log_scene ON recommendation_log(scene);
CREATE INDEX idx_recommendation_log_recommended_at ON recommendation_log(recommended_at);
CREATE INDEX idx_recommendation_log_clicked ON recommendation_log(clicked);
CREATE INDEX idx_recommendation_log_borrowed ON recommendation_log(borrowed);

-- 添加注释
COMMENT ON TABLE borrow_history IS '借阅历史表 - 用于推荐算法计算';
COMMENT ON TABLE user_similarity IS '用户相似度矩阵 - 预计算的用户间相似度';
COMMENT ON TABLE item_similarity IS '物品相似度矩阵 - 预计算的图书间相似度';
COMMENT ON TABLE recommendation_log IS '推荐日志 - 记录推荐结果和用户反馈';

COMMENT ON COLUMN borrow_history.implicit_rating IS '隐式评分：完成借阅=3，续借=4，提前归还=2，逾期=1';
COMMENT ON COLUMN user_similarity.similarity_score IS '余弦相似度，范围[0,1]';
COMMENT ON COLUMN item_similarity.similarity_score IS '调整余弦相似度，范围[0,1]';
COMMENT ON COLUMN recommendation_log.algorithm IS 'USER_CF/ITEM_CF/POPULAR/HYBRID/CONTENT';
COMMENT ON COLUMN recommendation_log.scene IS 'HOMEPAGE/DETAIL/SEARCH/TOPIC';
