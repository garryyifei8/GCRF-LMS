-- =====================================================
-- GCRF Library Management System - Chat Service
-- Database Migration Script V001 - Baseline
-- Create chat and FAQ related tables
-- =====================================================

-- FAQ分类表
CREATE TABLE IF NOT EXISTS faq_category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    icon        VARCHAR(200),
    sort_order  INTEGER      DEFAULT 0,
    status      INTEGER      NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP,
    CONSTRAINT uk_faq_category_code UNIQUE (code)
);

CREATE INDEX idx_faq_category_status ON faq_category(status);
CREATE INDEX idx_faq_category_sort   ON faq_category(sort_order);

COMMENT ON TABLE  faq_category             IS 'FAQ分类表';
COMMENT ON COLUMN faq_category.code        IS '分类编码，全局唯一';
COMMENT ON COLUMN faq_category.sort_order  IS '排序顺序，数值越小越靠前';
COMMENT ON COLUMN faq_category.status      IS '状态: 0-禁用, 1-启用';
COMMENT ON COLUMN faq_category.deleted_at  IS '软删除时间戳';

-- FAQ知识库表
CREATE TABLE IF NOT EXISTS faq_knowledge (
    id               BIGSERIAL PRIMARY KEY,
    category_id      BIGINT       NOT NULL,
    question         TEXT         NOT NULL,
    answer           TEXT         NOT NULL,
    keywords         JSONB,
    intent_tags      JSONB,
    priority         INTEGER      DEFAULT 0,
    view_count       INTEGER      DEFAULT 0,
    helpful_count    INTEGER      DEFAULT 0,
    unhelpful_count  INTEGER      DEFAULT 0,
    status           INTEGER      NOT NULL DEFAULT 1,
    created_by       BIGINT,
    updated_by       BIGINT,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP
);

CREATE INDEX idx_faq_knowledge_category ON faq_knowledge(category_id);
CREATE INDEX idx_faq_knowledge_status   ON faq_knowledge(status);
CREATE INDEX idx_faq_knowledge_priority ON faq_knowledge(priority DESC);
CREATE INDEX idx_faq_knowledge_keywords ON faq_knowledge USING GIN(keywords);
CREATE INDEX idx_faq_knowledge_tags     ON faq_knowledge USING GIN(intent_tags);

COMMENT ON TABLE  faq_knowledge                IS 'FAQ知识库表';
COMMENT ON COLUMN faq_knowledge.keywords       IS '关键词数组 (JSON Array)';
COMMENT ON COLUMN faq_knowledge.intent_tags    IS '意图标签数组 (JSON Array)';
COMMENT ON COLUMN faq_knowledge.priority       IS '优先级，值越高越优先匹配';
COMMENT ON COLUMN faq_knowledge.status         IS '状态: 0-禁用, 1-启用';
COMMENT ON COLUMN faq_knowledge.deleted_at     IS '软删除时间戳';

-- 意图定义表
CREATE TABLE IF NOT EXISTS chat_intent (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    code              VARCHAR(50)  NOT NULL,
    description       VARCHAR(500),
    patterns          JSONB,
    entities          JSONB,
    response_template TEXT,
    action_type       VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    action_params     JSONB,
    status            INTEGER      NOT NULL DEFAULT 1,
    created_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    CONSTRAINT uk_chat_intent_code UNIQUE (code)
);

CREATE INDEX idx_chat_intent_status ON chat_intent(status);

COMMENT ON TABLE  chat_intent               IS '意图定义表';
COMMENT ON COLUMN chat_intent.code          IS '意图编码，全局唯一';
COMMENT ON COLUMN chat_intent.patterns      IS '匹配模式数组 (JSON Array)';
COMMENT ON COLUMN chat_intent.entities      IS '关联实体数组 (JSON Array)';
COMMENT ON COLUMN chat_intent.action_type   IS '动作类型: FAQ_LOOKUP, API_CALL, TRANSFER, NONE';
COMMENT ON COLUMN chat_intent.action_params IS '动作参数 (JSON Object)';
COMMENT ON COLUMN chat_intent.status        IS '状态: 0-禁用, 1-启用';
COMMENT ON COLUMN chat_intent.deleted_at    IS '软删除时间戳';

-- 会话记录表
CREATE TABLE IF NOT EXISTS chat_session (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         VARCHAR(64)  NOT NULL,
    reader_id          BIGINT,
    start_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time           TIMESTAMP,
    message_count      INTEGER      DEFAULT 0,
    resolved           BOOLEAN      DEFAULT FALSE,
    satisfaction_score INTEGER      CHECK (satisfaction_score >= 1 AND satisfaction_score <= 5),
    feedback           TEXT,
    context            JSONB,
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_session_session_id UNIQUE (session_id)
);

CREATE INDEX idx_chat_session_reader     ON chat_session(reader_id);
CREATE INDEX idx_chat_session_start_time ON chat_session(start_time);
CREATE INDEX idx_chat_session_resolved   ON chat_session(resolved);

COMMENT ON TABLE  chat_session                    IS '会话记录表';
COMMENT ON COLUMN chat_session.session_id         IS '会话唯一ID (UUID)';
COMMENT ON COLUMN chat_session.reader_id          IS '读者ID，NULL 表示匿名用户';
COMMENT ON COLUMN chat_session.satisfaction_score IS '满意度评分 1-5';
COMMENT ON COLUMN chat_session.context            IS '会话上下文 (JSON Object)';

-- 会话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(64)   NOT NULL,
    role            VARCHAR(20)   NOT NULL,
    content         TEXT          NOT NULL,
    intent_code     VARCHAR(50),
    confidence      DECIMAL(5, 4) CHECK (confidence >= 0 AND confidence <= 1),
    matched_faq_id  BIGINT,
    metadata        JSONB,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_session    ON chat_message(session_id);
CREATE INDEX idx_chat_message_role       ON chat_message(role);
CREATE INDEX idx_chat_message_intent     ON chat_message(intent_code);
CREATE INDEX idx_chat_message_created_at ON chat_message(created_at);

COMMENT ON TABLE  chat_message               IS '会话消息表';
COMMENT ON COLUMN chat_message.role          IS '角色: user, assistant, system';
COMMENT ON COLUMN chat_message.confidence    IS '意图置信度 0~1';
COMMENT ON COLUMN chat_message.metadata      IS '消息元数据 (JSON Object)';

-- 会话反馈表
CREATE TABLE IF NOT EXISTS chat_feedback (
    id            BIGSERIAL PRIMARY KEY,
    session_id    VARCHAR(64) NOT NULL,
    message_id    BIGINT,
    faq_id        BIGINT,
    feedback_type VARCHAR(20) NOT NULL,
    comment       TEXT,
    reader_id     BIGINT,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_feedback_session  ON chat_feedback(session_id);
CREATE INDEX idx_chat_feedback_faq      ON chat_feedback(faq_id);
CREATE INDEX idx_chat_feedback_type     ON chat_feedback(feedback_type);
CREATE INDEX idx_chat_feedback_reader   ON chat_feedback(reader_id);

COMMENT ON TABLE  chat_feedback               IS '会话反馈表';
COMMENT ON COLUMN chat_feedback.feedback_type IS '反馈类型: helpful, unhelpful, report';

-- 热门问题统计表
CREATE TABLE IF NOT EXISTS hot_question_stats (
    id              BIGSERIAL PRIMARY KEY,
    question_text   TEXT        NOT NULL,
    normalized_text TEXT,
    faq_id          BIGINT,
    ask_count       INTEGER     DEFAULT 1,
    last_asked_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hot_question_ask_count    ON hot_question_stats(ask_count DESC);
CREATE INDEX idx_hot_question_last_asked   ON hot_question_stats(last_asked_at DESC);
CREATE INDEX idx_hot_question_faq          ON hot_question_stats(faq_id);

COMMENT ON TABLE  hot_question_stats                IS '热门问题统计表';
COMMENT ON COLUMN hot_question_stats.normalized_text IS '标准化后的问题文本，用于去重';
COMMENT ON COLUMN hot_question_stats.ask_count       IS '提问次数';
