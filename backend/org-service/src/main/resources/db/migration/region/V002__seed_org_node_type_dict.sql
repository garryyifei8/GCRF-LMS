INSERT INTO org_node_type(code, name, parent_types, display_order) VALUES
    ('REGION',     '教育局/区域', '{}',                                 10),
    ('DISTRICT',   '区/县',       '{REGION}',                           20),
    ('SCHOOL',     '学校',        '{REGION,DISTRICT}',                  30),
    ('SUB_SCHOOL', '分校',        '{SCHOOL}',                           40),
    ('BRANCH',     '分馆',        '{SCHOOL,SUB_SCHOOL}',                50),
    ('STAGE',      '学段',        '{SCHOOL,SUB_SCHOOL}',                60),
    ('GRADE',      '年级',        '{SCHOOL,SUB_SCHOOL,STAGE}',          70),
    ('CLASS',      '班级',        '{GRADE}',                            80)
ON CONFLICT (code) DO NOTHING;
