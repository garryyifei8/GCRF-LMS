-- Insert test readers data for reader_service
\c reader_service

-- Insert sample readers (30 readers with various types)
INSERT INTO readers (reader_id, card_number, reader_name, reader_type, gender, phone, email, department, major, grade, class_name, credit_score, max_borrow_quantity, card_status, issue_date, expire_date) VALUES
-- Students (20 readers)
('RD2024001', 'CARD001', '张伟', 'STUDENT', 'MALE', '13800138001', 'zhangwei@student.edu.cn', '计算机学院', '软件工程', '2021', '软件2101', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024002', 'CARD002', '李娜', 'STUDENT', 'FEMALE', '13800138002', 'lina@student.edu.cn', '计算机学院', '计算机科学与技术', '2021', '计科2102', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024003', 'CARD003', '王强', 'STUDENT', 'MALE', '13800138003', 'wangqiang@student.edu.cn', '电子工程学院', '电子信息工程', '2022', '电信2201', 95, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),
('RD2024004', 'CARD004', '刘芳', 'STUDENT', 'FEMALE', '13800138004', 'liufang@student.edu.cn', '外国语学院', '英语', '2022', '英语2201', 100, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),
('RD2024005', 'CARD005', '陈明', 'STUDENT', 'MALE', '13800138005', 'chenming@student.edu.cn', '经济管理学院', '工商管理', '2021', '工管2103', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024006', 'CARD006', '赵丽', 'STUDENT', 'FEMALE', '13800138006', 'zhaoli@student.edu.cn', '数学学院', '数学与应用数学', '2023', '数学2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024007', 'CARD007', '周涛', 'STUDENT', 'MALE', '13800138007', 'zhoutao@student.edu.cn', '物理学院', '应用物理学', '2023', '物理2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024008', 'CARD008', '吴静', 'STUDENT', 'FEMALE', '13800138008', 'wujing@student.edu.cn', '化学学院', '化学工程与工艺', '2022', '化工2202', 90, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),
('RD2024009', 'CARD009', '郑波', 'STUDENT', 'MALE', '13800138009', 'zhengbo@student.edu.cn', '计算机学院', '信息安全', '2021', '信安2101', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024010', 'CARD010', '孙琳', 'STUDENT', 'FEMALE', '13800138010', 'sunlin@student.edu.cn', '建筑学院', '建筑学', '2023', '建筑2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024011', 'CARD011', '朱杰', 'STUDENT', 'MALE', '13800138011', 'zhujie@student.edu.cn', '机械工程学院', '机械设计制造及其自动化', '2022', '机械2201', 100, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),
('RD2024012', 'CARD012', '许敏', 'STUDENT', 'FEMALE', '13800138012', 'xumin@student.edu.cn', '新闻传播学院', '新闻学', '2021', '新闻2101', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024013', 'CARD013', '何超', 'STUDENT', 'MALE', '13800138013', 'hechao@student.edu.cn', '法学院', '法学', '2023', '法学2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024014', 'CARD014', '韩雪', 'STUDENT', 'FEMALE', '13800138014', 'hanxue@student.edu.cn', '医学院', '临床医学', '2022', '临床2201', 100, 10, 'ACTIVE', '2022-09-01', '2027-06-30'),
('RD2024015', 'CARD015', '曹阳', 'STUDENT', 'MALE', '13800138015', 'caoyang@student.edu.cn', '计算机学院', '人工智能', '2023', 'AI2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024016', 'CARD016', '邓颖', 'STUDENT', 'FEMALE', '13800138016', 'dengying@student.edu.cn', '艺术学院', '视觉传达设计', '2021', '设计2101', 85, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024017', 'CARD017', '谢磊', 'STUDENT', 'MALE', '13800138017', 'xielei@student.edu.cn', '体育学院', '运动训练', '2022', '体育2201', 100, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),
('RD2024018', 'CARD018', '宋佳', 'STUDENT', 'FEMALE', '13800138018', 'songjia@student.edu.cn', '生命科学学院', '生物技术', '2023', '生物2301', 100, 10, 'ACTIVE', '2023-09-01', '2027-06-30'),
('RD2024019', 'CARD019', '梁晨', 'STUDENT', 'MALE', '13800138019', 'liangchen@student.edu.cn', '环境科学学院', '环境工程', '2021', '环工2101', 100, 10, 'ACTIVE', '2021-09-01', '2025-06-30'),
('RD2024020', 'CARD020', '董萍', 'STUDENT', 'FEMALE', '13800138020', 'dongping@student.edu.cn', '计算机学院', '数据科学与大数据技术', '2022', '数据2201', 100, 10, 'ACTIVE', '2022-09-01', '2026-06-30'),

-- Teachers (7 readers)
('RD2024101', 'CARD101', '李教授', 'TEACHER', 'MALE', '13900139001', 'liprof@university.edu.cn', '计算机学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2015-09-01', '2035-08-31'),
('RD2024102', 'CARD102', '王教授', 'TEACHER', 'FEMALE', '13900139002', 'wangprof@university.edu.cn', '数学学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2012-09-01', '2035-08-31'),
('RD2024103', 'CARD103', '张副教授', 'TEACHER', 'MALE', '13900139003', 'zhangasso@university.edu.cn', '电子工程学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2018-09-01', '2035-08-31'),
('RD2024104', 'CARD104', '刘讲师', 'TEACHER', 'FEMALE', '13900139004', 'liulect@university.edu.cn', '外国语学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2020-09-01', '2035-08-31'),
('RD2024105', 'CARD105', '陈副教授', 'TEACHER', 'MALE', '13900139005', 'chenasso@university.edu.cn', '经济管理学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2016-09-01', '2035-08-31'),
('RD2024106', 'CARD106', '赵讲师', 'TEACHER', 'FEMALE', '13900139006', 'zhaolect@university.edu.cn', '法学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2021-09-01', '2035-08-31'),
('RD2024107', 'CARD107', '周副教授', 'TEACHER', 'MALE', '13900139007', 'zhouasso@university.edu.cn', '物理学院', NULL, NULL, NULL, 100, 20, 'ACTIVE', '2017-09-01', '2035-08-31'),

-- Staff (3 readers)
('RD2024201', 'CARD201', '杨秘书', 'STAFF', 'FEMALE', '13800138201', 'yangsec@university.edu.cn', '教务处', NULL, NULL, NULL, 100, 15, 'ACTIVE', '2019-03-01', '2029-02-28'),
('RD2024202', 'CARD202', '徐主任', 'STAFF', 'MALE', '13800138202', 'xudir@university.edu.cn', '学生处', NULL, NULL, NULL, 100, 15, 'ACTIVE', '2015-06-01', '2035-05-31'),
('RD2024203', 'CARD203', '林老师', 'STAFF', 'FEMALE', '13800138203', 'linteacher@university.edu.cn', '图书馆', NULL, NULL, NULL, 100, 15, 'ACTIVE', '2018-09-01', '2035-08-31')

ON CONFLICT (reader_id) DO NOTHING;

-- Display summary
SELECT 'Readers inserted successfully!' as message;
SELECT COUNT(*) as total_readers FROM readers;
SELECT reader_type, COUNT(*) as count FROM readers GROUP BY reader_type ORDER BY reader_type;
