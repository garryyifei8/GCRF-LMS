-- =====================================================
-- GCRF Library Management System - Recommend Service
-- Database Migration Script V2
-- Insert sample data for testing
-- =====================================================

-- 插入示例借阅历史数据
-- 模拟10个读者和20本图书的借阅行为

-- 读者1: 偏好计算机类图书
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(1, 1, 'Python编程从入门到实践', 'TP311', '2025-01-15 10:00:00', '2025-02-01 10:00:00', 17, 3.5),
(1, 2, '深度学习', 'TP181', '2025-02-10 10:00:00', '2025-03-01 10:00:00', 19, 4.0),
(1, 3, '机器学习实战', 'TP181', '2025-03-05 10:00:00', '2025-03-20 10:00:00', 15, 3.8),
(1, 5, '数据结构与算法', 'TP311', '2025-04-01 10:00:00', '2025-04-15 10:00:00', 14, 3.2);

-- 读者2: 偏好计算机和数学类
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(2, 1, 'Python编程从入门到实践', 'TP311', '2025-01-20 10:00:00', '2025-02-05 10:00:00', 16, 3.6),
(2, 2, '深度学习', 'TP181', '2025-02-15 10:00:00', '2025-03-10 10:00:00', 23, 4.2),
(2, 4, '高等数学', 'O13', '2025-03-15 10:00:00', '2025-04-01 10:00:00', 17, 3.0),
(2, 6, '线性代数', 'O151', '2025-04-05 10:00:00', '2025-04-20 10:00:00', 15, 3.4);

-- 读者3: 偏好文学类
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(3, 10, '红楼梦', 'I242', '2025-01-10 10:00:00', '2025-02-10 10:00:00', 31, 4.5),
(3, 11, '西游记', 'I242', '2025-02-20 10:00:00', '2025-03-15 10:00:00', 23, 4.0),
(3, 12, '三国演义', 'I242', '2025-03-20 10:00:00', '2025-04-10 10:00:00', 21, 4.2),
(3, 13, '水浒传', 'I242', '2025-04-15 10:00:00', NULL, NULL, 3.0);

-- 读者4: 偏好计算机类（与读者1相似）
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(4, 1, 'Python编程从入门到实践', 'TP311', '2025-02-01 10:00:00', '2025-02-20 10:00:00', 19, 3.8),
(4, 3, '机器学习实战', 'TP181', '2025-03-01 10:00:00', '2025-03-18 10:00:00', 17, 4.0),
(4, 5, '数据结构与算法', 'TP311', '2025-03-25 10:00:00', '2025-04-10 10:00:00', 16, 3.5),
(4, 7, '计算机网络', 'TP393', '2025-04-15 10:00:00', '2025-05-01 10:00:00', 16, 3.3);

-- 读者5: 偏好文学（与读者3相似）
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(5, 10, '红楼梦', 'I242', '2025-01-25 10:00:00', '2025-02-25 10:00:00', 31, 4.8),
(5, 11, '西游记', 'I242', '2025-03-01 10:00:00', '2025-03-25 10:00:00', 24, 4.3),
(5, 14, '围城', 'I247', '2025-04-01 10:00:00', '2025-04-20 10:00:00', 19, 3.9);

-- 读者6: 混合偏好
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(6, 2, '深度学习', 'TP181', '2025-02-05 10:00:00', '2025-03-01 10:00:00', 24, 4.1),
(6, 10, '红楼梦', 'I242', '2025-03-10 10:00:00', '2025-04-05 10:00:00', 26, 4.4),
(6, 15, '经济学原理', 'F0', '2025-04-10 10:00:00', '2025-05-01 10:00:00', 21, 3.6);

-- 读者7: 偏好经济管理
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(7, 15, '经济学原理', 'F0', '2025-01-15 10:00:00', '2025-02-10 10:00:00', 26, 4.0),
(7, 16, '管理学', 'C93', '2025-02-20 10:00:00', '2025-03-15 10:00:00', 23, 3.8),
(7, 17, '金融学', 'F830', '2025-03-20 10:00:00', '2025-04-15 10:00:00', 26, 4.2);

-- 读者8: 偏好计算机和经济
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(8, 1, 'Python编程从入门到实践', 'TP311', '2025-02-10 10:00:00', '2025-03-01 10:00:00', 19, 3.7),
(8, 15, '经济学原理', 'F0', '2025-03-05 10:00:00', '2025-03-25 10:00:00', 20, 3.9),
(8, 18, '数据分析与决策', 'TP311', '2025-04-01 10:00:00', '2025-04-20 10:00:00', 19, 4.0);

-- 读者9: 新用户（借阅少，用于测试冷启动）
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(9, 19, '人工智能基础', 'TP18', '2025-04-15 10:00:00', NULL, NULL, 3.0);

-- 读者10: 活跃用户（借阅多）
INSERT INTO borrow_history (reader_id, book_id, book_title, category_code, borrow_time, return_time, borrow_days, implicit_rating)
VALUES
(10, 1, 'Python编程从入门到实践', 'TP311', '2025-01-05 10:00:00', '2025-01-20 10:00:00', 15, 3.9),
(10, 2, '深度学习', 'TP181', '2025-01-25 10:00:00', '2025-02-15 10:00:00', 21, 4.3),
(10, 3, '机器学习实战', 'TP181', '2025-02-20 10:00:00', '2025-03-10 10:00:00', 18, 4.1),
(10, 10, '红楼梦', 'I242', '2025-03-15 10:00:00', '2025-04-10 10:00:00', 26, 4.5),
(10, 15, '经济学原理', 'F0', '2025-04-15 10:00:00', '2025-05-05 10:00:00', 20, 3.8);

-- 预计算用户相似度（基于上述数据）
-- 读者1和读者4相似（都借阅了图书1,3,5）
INSERT INTO user_similarity (user_id_a, user_id_b, similarity_score, common_items_count, calculated_at)
VALUES
(1, 4, 0.85, 3, CURRENT_TIMESTAMP),
(1, 2, 0.72, 2, CURRENT_TIMESTAMP),
(1, 10, 0.68, 3, CURRENT_TIMESTAMP),
(2, 4, 0.45, 1, CURRENT_TIMESTAMP),
(3, 5, 0.92, 2, CURRENT_TIMESTAMP),
(3, 6, 0.35, 1, CURRENT_TIMESTAMP),
(6, 10, 0.55, 2, CURRENT_TIMESTAMP),
(7, 8, 0.48, 1, CURRENT_TIMESTAMP);

-- 预计算物品相似度
-- 图书1,3,5都是计算机类，被类似用户借阅
INSERT INTO item_similarity (book_id_a, book_id_b, similarity_score, common_users_count, calculated_at)
VALUES
(1, 3, 0.78, 3, CURRENT_TIMESTAMP),
(1, 5, 0.72, 2, CURRENT_TIMESTAMP),
(1, 2, 0.68, 4, CURRENT_TIMESTAMP),
(2, 3, 0.82, 3, CURRENT_TIMESTAMP),
(3, 5, 0.75, 2, CURRENT_TIMESTAMP),
(10, 11, 0.88, 2, CURRENT_TIMESTAMP),
(10, 12, 0.85, 1, CURRENT_TIMESTAMP),
(11, 12, 0.82, 1, CURRENT_TIMESTAMP),
(15, 16, 0.65, 1, CURRENT_TIMESTAMP),
(15, 17, 0.70, 1, CURRENT_TIMESTAMP);

-- 插入一些推荐日志记录（用于统计测试）
INSERT INTO recommendation_log (reader_id, book_id, score, algorithm, scene, reason, clicked, clicked_at, borrowed, borrowed_at, recommended_at)
VALUES
(1, 7, 0.75, 'USER_CF', 'HOMEPAGE', '与你阅读偏好相近的读者推荐', true, '2025-05-01 14:30:00', true, '2025-05-02 10:00:00', '2025-05-01 10:00:00'),
(1, 8, 0.68, 'ITEM_CF', 'DETAIL', '与你借阅过的图书相似', true, '2025-05-01 15:00:00', false, NULL, '2025-05-01 10:00:00'),
(2, 7, 0.72, 'HYBRID', 'HOMEPAGE', '综合推荐', false, NULL, false, NULL, '2025-05-01 10:00:00'),
(3, 14, 0.82, 'USER_CF', 'HOMEPAGE', '基于相似读者的借阅历史推荐', true, '2025-05-01 11:00:00', true, '2025-05-01 14:00:00', '2025-05-01 10:00:00'),
(4, 8, 0.70, 'POPULAR', 'HOMEPAGE', '近30天内有15位读者借阅', true, '2025-05-02 09:00:00', false, NULL, '2025-05-02 08:00:00');
