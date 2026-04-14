-- 图书分类初始数据 (基于中国图书馆分类法 CLC)
-- Date: 2025-11-12

-- 一级分类 (22个大类)
INSERT INTO book_category (id, parent_id, category_name, category_code, path, level, description, sort_order, status) VALUES
(1, NULL, '马克思主义、列宁主义、毛泽东思想、邓小平理论', 'A', '001', 1, '马列毛邓理论', 1, 'ACTIVE'),
(2, NULL, '哲学、宗教', 'B', '002', 1, '哲学与宗教类图书', 2, 'ACTIVE'),
(3, NULL, '社会科学总论', 'C', '003', 1, '社会科学综合类', 3, 'ACTIVE'),
(4, NULL, '政治、法律', 'D', '004', 1, '政治法律类图书', 4, 'ACTIVE'),
(5, NULL, '军事', 'E', '005', 1, '军事科学类图书', 5, 'ACTIVE'),
(6, NULL, '经济', 'F', '006', 1, '经济学类图书', 6, 'ACTIVE'),
(7, NULL, '文化、科学、教育、体育', 'G', '007', 1, '文教体育类', 7, 'ACTIVE'),
(8, NULL, '语言、文字', 'H', '008', 1, '语言文字类图书', 8, 'ACTIVE'),
(9, NULL, '文学', 'I', '009', 1, '文学类图书', 9, 'ACTIVE'),
(10, NULL, '艺术', 'J', '010', 1, '艺术类图书', 10, 'ACTIVE'),
(11, NULL, '历史、地理', 'K', '011', 1, '历史地理类图书', 11, 'ACTIVE'),
(12, NULL, '自然科学总论', 'N', '012', 1, '自然科学综合类', 12, 'ACTIVE'),
(13, NULL, '数理科学和化学', 'O', '013', 1, '数学、物理、化学', 13, 'ACTIVE'),
(14, NULL, '天文学、地球科学', 'P', '014', 1, '天文地学类', 14, 'ACTIVE'),
(15, NULL, '生物科学', 'Q', '015', 1, '生物学类图书', 15, 'ACTIVE'),
(16, NULL, '医药、卫生', 'R', '016', 1, '医学卫生类', 16, 'ACTIVE'),
(17, NULL, '农业科学', 'S', '017', 1, '农业类图书', 17, 'ACTIVE'),
(18, NULL, '工业技术', 'T', '018', 1, '工程技术类', 18, 'ACTIVE'),
(19, NULL, '交通运输', 'U', '019', 1, '交通类图书', 19, 'ACTIVE'),
(20, NULL, '航空、航天', 'V', '020', 1, '航空航天类', 20, 'ACTIVE'),
(21, NULL, '环境科学、安全科学', 'X', '021', 1, '环境安全类', 21, 'ACTIVE'),
(22, NULL, '综合性图书', 'Z', '022', 1, '综合类图书', 22, 'ACTIVE')
ON CONFLICT (category_code) DO NOTHING;

-- 二级分类示例 (部分常用类别)

-- 哲学、宗教 (B类)
INSERT INTO book_category (parent_id, category_name, category_code, path, level, description, sort_order, status) VALUES
(2, '哲学理论', 'B0', '002.001', 2, '哲学基础理论', 1, 'ACTIVE'),
(2, '世界哲学', 'B1', '002.002', 2, '世界各国哲学', 2, 'ACTIVE'),
(2, '中国哲学', 'B2', '002.003', 2, '中国哲学思想', 3, 'ACTIVE'),
(2, '亚洲哲学', 'B3', '002.004', 2, '亚洲各国哲学', 4, 'ACTIVE'),
(2, '非洲哲学', 'B4', '002.005', 2, '非洲哲学', 5, 'ACTIVE'),
(2, '欧洲哲学', 'B5', '002.006', 2, '欧洲哲学', 6, 'ACTIVE'),
(2, '大洋洲哲学', 'B6', '002.007', 2, '大洋洲哲学', 7, 'ACTIVE'),
(2, '美洲哲学', 'B7', '002.008', 2, '美洲哲学', 8, 'ACTIVE'),
(2, '思维科学', 'B80', '002.009', 2, '逻辑学等', 9, 'ACTIVE'),
(2, '心理学', 'B84', '002.010', 2, '心理学', 10, 'ACTIVE'),
(2, '宗教', 'B9', '002.011', 2, '各种宗教', 11, 'ACTIVE')
ON CONFLICT (category_code) DO NOTHING;

-- 文学 (I类) - 最常用
INSERT INTO book_category (parent_id, category_name, category_code, path, level, description, sort_order, status) VALUES
(9, '文学理论', 'I0', '009.001', 2, '文学基础理论', 1, 'ACTIVE'),
(9, '世界文学', 'I1', '009.002', 2, '世界文学作品', 2, 'ACTIVE'),
(9, '中国文学', 'I2', '009.003', 2, '中国文学作品', 3, 'ACTIVE'),
(9, '亚洲文学', 'I3', '009.004', 2, '亚洲各国文学', 4, 'ACTIVE'),
(9, '非洲文学', 'I4', '009.005', 2, '非洲文学', 5, 'ACTIVE'),
(9, '欧洲文学', 'I5', '009.006', 2, '欧洲文学', 6, 'ACTIVE'),
(9, '大洋洲文学', 'I6', '009.007', 2, '大洋洲文学', 7, 'ACTIVE'),
(9, '美洲文学', 'I7', '009.008', 2, '美洲文学', 8, 'ACTIVE')
ON CONFLICT (category_code) DO NOTHING;

-- 工业技术 (T类) - 包含计算机
INSERT INTO book_category (parent_id, category_name, category_code, path, level, description, sort_order, status) VALUES
(18, '工业技术总论', 'TB', '018.001', 2, '工业技术综合', 1, 'ACTIVE'),
(18, '矿业工程', 'TD', '018.002', 2, '采矿工程', 2, 'ACTIVE'),
(18, '石油、天然气工业', 'TE', '018.003', 2, '石油天然气', 3, 'ACTIVE'),
(18, '冶金工业', 'TF', '018.004', 2, '冶金技术', 4, 'ACTIVE'),
(18, '金属学与金属工艺', 'TG', '018.005', 2, '金属加工', 5, 'ACTIVE'),
(18, '机械、仪表工业', 'TH', '018.006', 2, '机械制造', 6, 'ACTIVE'),
(18, '武器工业', 'TJ', '018.007', 2, '武器制造', 7, 'ACTIVE'),
(18, '能源与动力工程', 'TK', '018.008', 2, '能源动力', 8, 'ACTIVE'),
(18, '原子能技术', 'TL', '018.009', 2, '核能技术', 9, 'ACTIVE'),
(18, '电工技术', 'TM', '018.010', 2, '电气工程', 10, 'ACTIVE'),
(18, '无线电电子学、电信技术', 'TN', '018.011', 2, '电子通信', 11, 'ACTIVE'),
(18, '自动化技术、计算机技术', 'TP', '018.012', 2, '计算机与自动化', 12, 'ACTIVE'),
(18, '化学工业', 'TQ', '018.013', 2, '化工技术', 13, 'ACTIVE'),
(18, '轻工业、手工业', 'TS', '018.014', 2, '轻工制造', 14, 'ACTIVE'),
(18, '建筑科学', 'TU', '018.015', 2, '建筑工程', 15, 'ACTIVE'),
(18, '水利工程', 'TV', '018.016', 2, '水利水电', 16, 'ACTIVE')
ON CONFLICT (category_code) DO NOTHING;

-- 三级分类示例 (计算机类 - 最常用)
INSERT INTO book_category (parent_id, category_name, category_code, path, level, description, sort_order, status)
SELECT id, '计算机理论', 'TP30', '018.012.001', 3, '计算机科学理论', 1, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '计算机技术', 'TP31', '018.012.002', 3, '计算机技术', 2, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '程序设计', 'TP311', '018.012.003', 3, '编程语言与技术', 3, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '软件工程', 'TP311.5', '018.012.004', 3, '软件工程方法', 4, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '数据库', 'TP311.13', '018.012.005', 3, '数据库技术', 5, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '算法与数据结构', 'TP312', '018.012.006', 3, '算法与数据结构', 6, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '计算机网络', 'TP393', '018.012.007', 3, '计算机网络技术', 7, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
UNION ALL
SELECT id, '人工智能', 'TP18', '018.012.008', 3, '人工智能与机器学习', 8, 'ACTIVE'
FROM book_category WHERE category_code = 'TP'
ON CONFLICT (category_code) DO NOTHING;

-- 更新分类统计
UPDATE book_category SET
    child_count = (SELECT COUNT(*) FROM book_category c WHERE c.parent_id = book_category.id AND c.deleted_at IS NULL),
    book_count = 0
WHERE deleted_at IS NULL;

-- 重置序列
SELECT setval('book_category_id_seq', (SELECT MAX(id) FROM book_category));
