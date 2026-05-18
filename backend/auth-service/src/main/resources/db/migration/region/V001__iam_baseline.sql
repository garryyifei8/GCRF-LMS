-- backend/auth-service/src/main/resources/db/migration/region/V001__iam_baseline.sql
-- gcrf_region.users + auth_role / auth_permission / auth_role_permission / auth_user_role + seed
-- Owned by auth-service. Other services using gcrf_region (org-service, opac-service) have their own
-- flyway_schema_history_* per ADR-005.

-- ============ Ensure schema exists ============
CREATE SCHEMA IF NOT EXISTS gcrf_region;

-- ============ users (从 auth_service.users 迁移而来 + 扩展) ============

CREATE TABLE IF NOT EXISTS gcrf_region.users (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(50) NOT NULL UNIQUE,
    username           VARCHAR(100) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(100),
    phone              VARCHAR(20),
    user_type          VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar_url         VARCHAR(500),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_time    TIMESTAMP WITHOUT TIME ZONE,
    last_login_ip      VARCHAR(50),
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP WITHOUT TIME ZONE,
    org_node_id        BIGINT,
    school_id          BIGINT,
    tenant_schema      VARCHAR(64),
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMP WITHOUT TIME ZONE
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_alive
    ON gcrf_region.users (email) WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_org_node  ON gcrf_region.users (org_node_id);
CREATE INDEX IF NOT EXISTS idx_users_school    ON gcrf_region.users (school_id);
CREATE INDEX IF NOT EXISTS idx_users_status    ON gcrf_region.users (status);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON gcrf_region.users (user_type);

-- ============ auth_role ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_role (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    scope_default VARCHAR(20) NOT NULL,
    is_system     BOOLEAN NOT NULL DEFAULT false,
    school_id     BIGINT,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_role_school ON gcrf_region.auth_role (school_id);

INSERT INTO gcrf_region.auth_role (code, name, description, scope_default, is_system, sort_order) VALUES
    ('REGION_ADMIN',        '区域超管',     '教育局全权',               'REGION', true, 10),
    ('REGION_LIBRARIAN',    '区域馆员',     '区域读取 + 馆藏标准维护',   'REGION', true, 20),
    ('SCHOOL_ADMIN',        '学校管理员',   '本校全权',                  'SCHOOL', true, 30),
    ('SCHOOL_LIBRARY_HEAD', '学校馆长',     '本校馆藏 + 用户 + 报表',    'SCHOOL', true, 40),
    ('LIBRARIAN',           '学校馆员',     '本校借还 + 编目',           'SCHOOL', true, 50),
    ('OPERATOR',            '操作员',       '借还工位',                  'SCHOOL', true, 60),
    ('TEACHER',             '教师',         '借阅 + 班级阅读报表',       'CLASS',  true, 70),
    ('STUDENT',             '学生',         '借阅 + 预约 + 测评',        'SELF',   true, 80),
    ('PARENT',              '家长',         '查阅子女阅读情况',          'SELF',   true, 90),
    ('GUEST',               '游客',         'OPAC 只读',                 'SELF',   true, 100)
ON CONFLICT (code) DO NOTHING;

-- ============ auth_permission ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_permission (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(50) NOT NULL,
    action      VARCHAR(50) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_perm_module ON gcrf_region.auth_permission (module);

INSERT INTO gcrf_region.auth_permission (code, module, action, name, sort_order) VALUES
    ('book.read',         'book',         'read',  '图书查询',  10),
    ('book.write',        'book',         'write', '图书编辑',  20),
    ('circulation.read',  'circulation',  'read',  '流通查询',  30),
    ('circulation.write', 'circulation',  'write', '借还操作',  40),
    ('reader.read',       'reader',       'read',  '读者查询',  50),
    ('reader.write',      'reader',       'write', '读者编辑',  60),
    ('system.read',       'system',       'read',  '系统查询',  70),
    ('system.write',      'system',       'write', '系统编辑',  80),
    ('analytics.read',    'analytics',    'read',  '报表查询',  90),
    ('org.read',          'org',          'read',  '组织查询', 100),
    ('org.write',         'org',          'write', '组织编辑', 110),
    ('opac.read',         'opac',         'read',  '检索访问', 120)
ON CONFLICT (code) DO NOTHING;

-- ============ auth_role_permission ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_role_permission (
    role_id       BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES gcrf_region.auth_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Seed role-permission mappings using PL/pgSQL for idempotent assignment.
DO $$
DECLARE rid BIGINT;
BEGIN
    -- REGION_ADMIN: ALL 12 permissions
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'REGION_ADMIN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, p.id FROM gcrf_region.auth_permission p
        ON CONFLICT DO NOTHING;

    -- REGION_LIBRARIAN: book.read, analytics.read, org.read, opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'REGION_LIBRARIAN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('book.read','analytics.read','org.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- SCHOOL_ADMIN: book/circulation/reader/system .* + analytics.read + org.read + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'SCHOOL_ADMIN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader','system')
            OR code IN ('analytics.read','org.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- SCHOOL_LIBRARY_HEAD: book/circulation/reader.* + analytics.read + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'SCHOOL_LIBRARY_HEAD';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader')
            OR code IN ('analytics.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- LIBRARIAN: book/circulation/reader.* + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'LIBRARIAN';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE module IN ('book','circulation','reader') OR code = 'opac.read'
        ON CONFLICT DO NOTHING;

    -- OPERATOR: circulation.write + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'OPERATOR';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('circulation.write','opac.read')
        ON CONFLICT DO NOTHING;

    -- TEACHER: book.read + analytics.read + opac.read
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'TEACHER';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission
         WHERE code IN ('book.read','analytics.read','opac.read')
        ON CONFLICT DO NOTHING;

    -- STUDENT / PARENT: book.read + opac.read
    FOR rid IN
        SELECT id FROM gcrf_region.auth_role WHERE code IN ('STUDENT','PARENT')
    LOOP
        INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
            SELECT rid, id FROM gcrf_region.auth_permission
             WHERE code IN ('book.read','opac.read')
            ON CONFLICT DO NOTHING;
    END LOOP;

    -- GUEST: opac.read only
    SELECT id INTO rid FROM gcrf_region.auth_role WHERE code = 'GUEST';
    INSERT INTO gcrf_region.auth_role_permission(role_id, permission_id)
        SELECT rid, id FROM gcrf_region.auth_permission WHERE code = 'opac.read'
        ON CONFLICT DO NOTHING;
END $$;

-- ============ auth_user_role ============

CREATE TABLE IF NOT EXISTS gcrf_region.auth_user_role (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES gcrf_region.users(id) ON DELETE CASCADE,
    role_id        BIGINT NOT NULL REFERENCES gcrf_region.auth_role(id) ON DELETE CASCADE,
    school_id      BIGINT,
    scope_override VARCHAR(20),
    scope_path     VARCHAR(500),
    assigned_by    BIGINT,
    assigned_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP WITHOUT TIME ZONE
);
-- PG 15+ supports NULLS NOT DISTINCT — region-level assignment (school_id=NULL) treated as duplicate.
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_role
    ON gcrf_region.auth_user_role (user_id, role_id, school_id) NULLS NOT DISTINCT;
CREATE INDEX IF NOT EXISTS idx_user_role_user   ON gcrf_region.auth_user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_school ON gcrf_region.auth_user_role (school_id);

-- ============ admin user + role bootstrap ============
-- username='admin' is the login name; BCrypt of "admin123" (10 rounds, verified).
INSERT INTO gcrf_region.users (user_id, username, password, email, user_type, status) VALUES
    ('admin', 'admin',
     '$2a$10$1zgJjT1EN3pNQKf.uc1ye.fPoc08jBsrT1cKkkKjW8hefA/xsCsmm',
     'admin@gcrf.edu.cn', 'ADMIN', 'ACTIVE')
ON CONFLICT (user_id) DO NOTHING;

-- Bind admin -> REGION_ADMIN (idempotent).
INSERT INTO gcrf_region.auth_user_role (user_id, role_id, assigned_by)
    SELECT u.id, r.id, u.id
      FROM gcrf_region.users u, gcrf_region.auth_role r
     WHERE u.user_id = 'admin' AND r.code = 'REGION_ADMIN'
ON CONFLICT DO NOTHING;
