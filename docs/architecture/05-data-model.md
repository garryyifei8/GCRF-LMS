# 数据模型

**日期：** 2026-04-30
**状态：** Approved
**关联：** [01-multi-tenant-isolation](01-multi-tenant-isolation.md)

---

## 1. 物理布局

```
PostgreSQL 实例
├── public                     ── Flyway history
├── gcrf_region                ── 区域公共 schema
└── school_NNN                 ── 每校独立 schema
```

## 2. 区域公共 schema (`gcrf_region`)

### 2.1 组织树

```sql
CREATE TABLE gcrf_region.org_node (
  id              BIGSERIAL PRIMARY KEY,
  parent_id       BIGINT REFERENCES gcrf_region.org_node(id),
  type            VARCHAR(30) NOT NULL,        -- REGION/DISTRICT/SCHOOL/SUB_SCHOOL/BRANCH/STAGE/GRADE/CLASS
  name            VARCHAR(100) NOT NULL,
  code            VARCHAR(50) UNIQUE,
  path            LTREE NOT NULL,              -- e.g. '100.001.s_001'
  tenant_schema   VARCHAR(50),                 -- 仅 SCHOOL+ 节点持有
  status          VARCHAR(20) DEFAULT 'ACTIVE',
  metadata        JSONB,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_org_path ON gcrf_region.org_node USING GIST (path);
CREATE INDEX idx_org_parent ON gcrf_region.org_node(parent_id);

CREATE TABLE gcrf_region.org_node_type (
  code           VARCHAR(30) PRIMARY KEY,
  name           VARCHAR(50),
  parent_types   TEXT[],         -- 允许的父类型
  display_order  INT
);
```

### 2.2 用户与权限

```sql
CREATE TABLE gcrf_region.user (
  id              BIGSERIAL PRIMARY KEY,
  login           VARCHAR(50) UNIQUE NOT NULL,
  password_hash   VARCHAR(255),
  identity_type   VARCHAR(30) NOT NULL,
  region_id       BIGINT,
  school_id       BIGINT,
  org_node_path   LTREE,
  email           VARCHAR(100),
  phone           VARCHAR(20),
  wx_openid       VARCHAR(64),
  wx_unionid      VARCHAR(64),
  card_number     VARCHAR(30),
  status          VARCHAR(20) DEFAULT 'ACTIVE',
  last_login_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_user_school ON gcrf_region.user(school_id);
CREATE INDEX idx_user_wx ON gcrf_region.user(wx_unionid);

CREATE TABLE gcrf_region.role (
  id          BIGSERIAL PRIMARY KEY,
  code        VARCHAR(50) UNIQUE,
  name        VARCHAR(100),
  is_builtin  BOOLEAN DEFAULT FALSE,
  scope       VARCHAR(20) DEFAULT 'SCHOOL',
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE gcrf_region.permission (
  id      BIGSERIAL PRIMARY KEY,
  code    VARCHAR(100) UNIQUE,
  module  VARCHAR(50),
  name    VARCHAR(100)
);

CREATE TABLE gcrf_region.role_permission (
  role_id        BIGINT REFERENCES gcrf_region.role(id),
  permission_id  BIGINT REFERENCES gcrf_region.permission(id),
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE gcrf_region.user_role (
  user_id          BIGINT REFERENCES gcrf_region.user(id),
  role_id          BIGINT REFERENCES gcrf_region.role(id),
  data_scope       VARCHAR(20) DEFAULT 'SELF',
  scope_node_path  LTREE,
  granted_at       TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, role_id)
);
```

### 2.3 家长 - 子女关联

```sql
CREATE TABLE gcrf_region.parent_student_link (
  id                BIGSERIAL PRIMARY KEY,
  parent_user_id    BIGINT NOT NULL,
  student_user_id   BIGINT NOT NULL,
  relation          VARCHAR(20),        -- FATHER/MOTHER/GUARDIAN
  verified          BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(parent_user_id, student_user_id)
);
```

### 2.4 馆藏标准

```sql
CREATE TABLE gcrf_region.collection_standard (
  id            BIGSERIAL PRIMARY KEY,
  standard_key  VARCHAR(50),       -- generic_per_capita, borrow_rate, ...
  school_type   VARCHAR(20),       -- PRIMARY/JUNIOR/SENIOR
  target_value  NUMERIC(12,2),
  unit          VARCHAR(20),
  description   TEXT,
  effective_at  TIMESTAMPTZ DEFAULT NOW(),
  region_id     BIGINT,
  UNIQUE(standard_key, school_type, region_id)
);

CREATE TABLE gcrf_region.collection_standard_check (
  id            BIGSERIAL PRIMARY KEY,
  org_node_id   BIGINT,
  standard_key  VARCHAR(50),
  actual_value  NUMERIC(12,2),
  qualified     BOOLEAN,
  checked_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### 2.5 不适宜书库

```sql
CREATE TABLE gcrf_region.inappropriate_book (
  id          BIGSERIAL PRIMARY KEY,
  isbn        VARCHAR(20) NOT NULL,
  title       VARCHAR(500),
  author      VARCHAR(200),
  reason      TEXT,
  source      VARCHAR(50),     -- NATIONAL_LIST / REGION_CUSTOM
  valid_from  DATE,
  created_by  BIGINT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_inappropriate_isbn ON gcrf_region.inappropriate_book(isbn);

CREATE TABLE gcrf_region.inappropriate_takedown_log (
  id              BIGSERIAL PRIMARY KEY,
  inappropriate_id BIGINT,
  school_id       BIGINT,
  copy_id         BIGINT,
  takedown_at     TIMESTAMPTZ DEFAULT NOW(),
  takedown_by     BIGINT
);
```

### 2.6 区域配置 + 数据密钥

```sql
CREATE TABLE gcrf_region.region_config (
  config_key    VARCHAR(100) PRIMARY KEY,
  config_value  TEXT,
  region_id     BIGINT,
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE gcrf_region.dashboard_key (
  key             VARCHAR(64) PRIMARY KEY,
  dashboard_id    BIGINT,
  scope_node_path LTREE,
  expires_at      TIMESTAMPTZ,
  created_by      BIGINT,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
```

## 3. 学校 schema (`school_NNN`)

### 3.1 馆藏

```sql
CREATE TABLE ${schema}.book_catalog (
  id              BIGSERIAL PRIMARY KEY,
  isbn            VARCHAR(20),
  title           VARCHAR(500) NOT NULL,
  author          VARCHAR(500),
  publisher       VARCHAR(200),
  pub_year        SMALLINT,
  classification  VARCHAR(50),     -- 中图法分类号
  category        VARCHAR(50),     -- 5 大类
  marc_data       JSONB,
  cover_url       VARCHAR(500),
  abstract        TEXT,
  total_count     INT DEFAULT 0,
  available_count INT DEFAULT 0,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_catalog_isbn ON ${schema}.book_catalog(isbn);
CREATE INDEX idx_catalog_clc ON ${schema}.book_catalog(classification);

CREATE TABLE ${schema}.collection_location (
  id          BIGSERIAL PRIMARY KEY,
  parent_id   BIGINT,
  name        VARCHAR(100),
  path        LTREE
);

CREATE TABLE ${schema}.book_copy (
  id              BIGSERIAL PRIMARY KEY,
  catalog_id      BIGINT REFERENCES ${schema}.book_catalog(id),
  barcode         VARCHAR(50) UNIQUE NOT NULL,
  call_no         VARCHAR(100),    -- 索书号
  location_id     BIGINT REFERENCES ${schema}.collection_location(id),
  status          VARCHAR(20) DEFAULT 'IN',  -- IN/OUT/LOST/DAMAGED/RETIRED
  acquired_at     DATE,
  price           NUMERIC(10,2),
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_copy_status ON ${schema}.book_copy(status);
```

### 3.2 读者

```sql
CREATE TABLE ${schema}.reader (
  id              BIGSERIAL PRIMARY KEY,
  user_id         BIGINT NOT NULL,           -- FK gcrf_region.user
  card_number     VARCHAR(30) UNIQUE,
  grade           VARCHAR(20),
  class           VARCHAR(20),
  max_borrow      INT DEFAULT 5,
  borrow_days     INT DEFAULT 30,
  status          VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE/FROZEN/CANCELLED
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
```

### 3.3 流通

```sql
CREATE TABLE ${schema}.borrow_record (
  id                BIGSERIAL PRIMARY KEY,
  reader_id         BIGINT REFERENCES ${schema}.reader(id),
  copy_id           BIGINT REFERENCES ${schema}.book_copy(id),
  borrow_at         TIMESTAMPTZ NOT NULL,
  due_at            TIMESTAMPTZ NOT NULL,
  return_at         TIMESTAMPTZ,
  renew_count       INT DEFAULT 0,
  fine_amount       NUMERIC(10,2) DEFAULT 0,
  operator_id       BIGINT,
  idempotency_key   VARCHAR(120) UNIQUE,
  source            VARCHAR(20) DEFAULT 'CAMPUS', -- CAMPUS/EDGE/WX/OPAC
  created_at        TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_borrow_reader ON ${schema}.borrow_record(reader_id);
CREATE INDEX idx_borrow_active ON ${schema}.borrow_record(reader_id, return_at) WHERE return_at IS NULL;

CREATE TABLE ${schema}.reservation (
  id          BIGSERIAL PRIMARY KEY,
  catalog_id  BIGINT,
  reader_id   BIGINT,
  status      VARCHAR(20),  -- PENDING/READY/PICKED/EXPIRED/CANCELLED
  hold_until  TIMESTAMPTZ,
  notified    BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);
```

### 3.4 智能分析

```sql
CREATE TABLE ${schema}.purchase_suggestion (
  id            BIGSERIAL PRIMARY KEY,
  isbn          VARCHAR(20),
  title         VARCHAR(500),
  score         NUMERIC(5,2),
  source        VARCHAR(50),  -- BORROW_HOTNESS / OFFICIAL_LIST / READER_RECOMMEND
  reason        TEXT,
  status        VARCHAR(20) DEFAULT 'PENDING',  -- PENDING/PURCHASED/REJECTED
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ${schema}.collection_analysis_report (
  id                BIGSERIAL PRIMARY KEY,
  generated_at      TIMESTAMPTZ DEFAULT NOW(),
  score_overall     NUMERIC(5,2),
  score_quantity    NUMERIC(5,2),
  score_quality     NUMERIC(5,2),
  score_structure   NUMERIC(5,2),
  dimensions        JSONB,
  generated_by      BIGINT
);
```

### 3.5 阅读测评

```sql
CREATE TABLE ${schema}.reading_test (
  id            BIGSERIAL PRIMARY KEY,
  title         VARCHAR(200),
  grade         VARCHAR(20),
  duration_min  INT,
  questions     JSONB,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE ${schema}.reading_test_result (
  id              BIGSERIAL PRIMARY KEY,
  test_id         BIGINT,
  reader_id       BIGINT,
  score           NUMERIC(5,2),
  answers         JSONB,
  ability_analysis JSONB,
  submitted_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### 3.6 微信进馆预约

```sql
CREATE TABLE ${schema}.wx_visit_reservation (
  id            BIGSERIAL PRIMARY KEY,
  reader_id     BIGINT,
  slot_date     DATE,
  slot_period   VARCHAR(20),     -- MORNING/AFTERNOON/EVENING
  status        VARCHAR(20),     -- BOOKED/CHECKED_IN/CANCELLED/NOSHOW
  qr_token      VARCHAR(64),     -- 核销二维码
  created_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### 3.7 盘点 / 评论 / 反馈（已有表，省略）

## 4. ES 索引（跨校检索）

### 4.1 索引模板

```json
PUT _index_template/gcrf-book-template
{
  "index_patterns": ["gcrf-book-*"],
  "template": {
    "settings": { "number_of_shards": 1, "analysis": { "analyzer": "ik_max_word" } },
    "mappings": {
      "properties": {
        "school_id": { "type": "keyword" },
        "isbn": { "type": "keyword" },
        "title": { "type": "text", "analyzer": "ik_max_word" },
        "author": { "type": "text" },
        "classification": { "type": "keyword" },
        "available_count": { "type": "integer" }
      }
    }
  }
}
```

### 4.2 索引命名

- 每校：`gcrf-book-school-001`, `gcrf-book-school-002` ...
- 别名：`gcrf-book-region` 指向所有学校 index → OPAC 跨校检索

### 4.3 同步

- `book-service` 维护 PG → ES 实时同步（Debezium 或应用层 outbox 模式）
- 每 5 分钟一次 refresh

## 5. Edge Agent 本地 SQLite Schema

```sql
-- 与云端结构对齐的精简版
CREATE TABLE reader (
  id INTEGER PRIMARY KEY,
  card_number TEXT UNIQUE,
  name TEXT,
  grade TEXT,
  class TEXT,
  max_borrow INTEGER DEFAULT 5,
  status TEXT DEFAULT 'ACTIVE',
  updated_at INTEGER
);

CREATE TABLE book_copy (
  id INTEGER PRIMARY KEY,
  barcode TEXT UNIQUE,
  title TEXT,
  status TEXT DEFAULT 'IN',
  updated_at INTEGER
);

CREATE TABLE borrow_record (
  id TEXT PRIMARY KEY,         -- idempotencyKey
  reader_id INTEGER,
  copy_id INTEGER,
  borrow_at INTEGER,
  due_at INTEGER,
  return_at INTEGER,
  sync_status TEXT DEFAULT 'PENDING',  -- PENDING/SYNCED/REJECTED
  created_at INTEGER
);

CREATE TABLE pending_queue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT,            -- BORROW_REQUEST / RETURN_REQUEST / ...
  payload TEXT,         -- JSON
  created_at INTEGER,
  retry_count INTEGER DEFAULT 0
);

CREATE TABLE config (
  key TEXT PRIMARY KEY,
  value TEXT,
  updated_at INTEGER
);
```

## 6. 关联文档

- [01-multi-tenant-isolation](01-multi-tenant-isolation.md)
- [03-edge-agent-sync-protocol](03-edge-agent-sync-protocol.md)
- [06-api-spec](06-api-spec.md)
