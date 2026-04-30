-- =========================================
-- V004 品牌配置（多租户系统名称/Logo）
-- 由超级管理员在 系统设置 中维护，所有学校登录后看到的品牌信息
-- =========================================

INSERT INTO system_config (config_key, config_value, description) VALUES
('brand_name', '国创睿峰智能图书馆', '系统品牌名称（显示在登录页/侧边栏/标题栏）'),
('brand_subtitle', '智慧图书馆管理平台', '品牌副标题/Slogan'),
('brand_logo_url', '', '自定义品牌 Logo URL（为空则使用系统内置 SVG）'),
('brand_login_title', '欢迎使用智慧图书馆系统', '登录页大标题'),
('brand_login_subtitle', 'AI 驱动的现代化图书管理平台', '登录页副标题')
ON CONFLICT DO NOTHING;
